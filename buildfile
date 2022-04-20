require 'buildr/single_intermediate_layout'
require 'buildr/top_level_generate_dir'
require 'buildr/git_auto_version'
require 'buildr/gpg'

DEPS=[:getopt4j,
      :sisu_plexus,
      :maven_core,
      :guava,
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
  compile.options.source = '1.8'
  compile.options.target = '1.8'
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
  package(:sources)
  package(:javadoc)
end
