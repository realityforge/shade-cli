require 'buildr/single_intermediate_layout'
require 'buildr/top_level_generate_dir'
require 'buildr/git_auto_version'
require 'buildr/gpg'

DEPS = [:getopt4j,
        :commons_collections4,
        :slf4j_api,
        :slf4j_jdk14,
        :sisu_plexus,
        :maven_core,
        :guava,
        :jdependency,
        :asm,
        :asm_tree,
        :maven_plugin_api,
        :plexus_utils,
        :asm_commons,
        :plexus_component_annotations,
        :maven_shade_plugin]

desc 'CLI Wrapper For Maven Shade tool'
define 'shade-cli' do
  project.group = 'org.realityforge.shade'
  compile.options.source = '17'
  compile.options.target = '17'
  compile.options.lint = 'all'

  project.version = ENV['PRODUCT_VERSION'] if ENV['PRODUCT_VERSION']

  pom.add_apache_v2_license
  pom.add_github_project('realityforge/shade')
  pom.add_developer('realityforge', 'Peter Donald', 'peter@realityforge.org')

  compile.with DEPS

  doc.options.merge!('Xdoclint:none' => true)

  package(:jar).tap do |jar|
    jar.with :manifest => { 'Main-Class' => 'org.realityforge.shade.Main' }
    jar.merge(artifacts(DEPS))
  end
  package(:jar, :classifier => 'all').tap do |jar|
    jar.with :manifest => { 'Main-Class' => 'org.realityforge.shade.Main' }
    jar.merge(artifacts(DEPS))
    jar.enhance do
      jar.enhance do |f|
        args = []
        args << Java::Commands.path_to_bin('java')
        args << '-jar'
        args << f.to_s
        args << '--input'
        args << f.to_s
        args << '--output'
        args << "#{f}.out"
        args << "-rorg.apache=org.realityforge.shade.vendor.org.apache"
        args << "-rcom.google=org.realityforge.shade.vendor.com.google"
        args << "-rorg.codehaus=org.realityforge.shade.vendor.org.codehaus"
        args << "-rorg.objectweb=org.realityforge.shade.vendor.org.objectweb"
        args << "-rorg.realityforge.getopt4j=org.realityforge.shade.vendor.org.realityforge.getopt4j"
        args << "-rorg.eclipse=org.realityforge.shade.vendor.org.eclipse"
        args << "-rorg.slf4j=org.realityforge.shade.vendor.org.slf4j"
        args << "-rorg.vafer=org.realityforge.shade.vendor.org.vafer"
        puts args.join(' ')
        sh args.join(' ')
        FileUtils.mv "#{f}.out", f.to_s
      end
    end
  end
  package(:sources)
  package(:javadoc)
end
