package org.realityforge.shade;

import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.apache.maven.plugins.shade.DefaultShader;
import org.apache.maven.plugins.shade.ShadeRequest;
import org.apache.maven.plugins.shade.relocation.Relocator;
import org.apache.maven.plugins.shade.relocation.SimpleRelocator;
import org.realityforge.getopt4j.CLArgsParser;
import org.realityforge.getopt4j.CLOption;
import org.realityforge.getopt4j.CLOptionDescriptor;
import org.realityforge.getopt4j.CLUtil;

/**
 * The entry point in which to run the tool.
 */
public class Main
{
  private static final int HELP_OPT = 1;
  private static final int QUIET_OPT = 'q';
  private static final int VERBOSE_OPT = 'v';
  private static final int INPUT_OPT = 2;
  private static final int OUTPUT_OPT = 3;
  private static final int RELOCATION_OPT = 'r';
  private static final CLOptionDescriptor[] OPTIONS = new CLOptionDescriptor[]{
    new CLOptionDescriptor( "input",
                            CLOptionDescriptor.ARGUMENT_REQUIRED,
                            INPUT_OPT,
                            "The input jar." ),
    new CLOptionDescriptor( "output",
                            CLOptionDescriptor.ARGUMENT_REQUIRED,
                            OUTPUT_OPT,
                            "The output jar with relocation rules applied." ),
    new CLOptionDescriptor( "relocation",
                            CLOptionDescriptor.ARGUMENTS_REQUIRED_2 | CLOptionDescriptor.DUPLICATES_ALLOWED,
                            RELOCATION_OPT,
                            "A relocation to apply in the form inputPattern=outputPattern." ),
    new CLOptionDescriptor( "help",
                            CLOptionDescriptor.ARGUMENT_DISALLOWED,
                            HELP_OPT,
                            "print this message and exit" ),
    new CLOptionDescriptor( "quiet",
                            CLOptionDescriptor.ARGUMENT_DISALLOWED,
                            QUIET_OPT,
                            "Do not output unless an error occurs, just return 0 on no difference.",
                            new int[]{ VERBOSE_OPT } ),
    new CLOptionDescriptor( "verbose",
                            CLOptionDescriptor.ARGUMENT_DISALLOWED,
                            VERBOSE_OPT,
                            "Verbose output of differences.",
                            new int[]{ QUIET_OPT } ),
    };
  private static final int SUCCESS_EXIT_CODE = 0;
  private static final int ERROR_PARSING_ARGS_EXIT_CODE = 2;
  private static final int ERROR_OTHER_EXIT_CODE = 4;
  private static final Logger c_logger = Logger.getAnonymousLogger();
  private static File c_input;
  private static File c_output;
  private static final Map<String, String> c_relocations = new HashMap<>();

  public static void main( final String[] args )
  {
    setupLogger();
    if ( !processOptions( args ) )
    {
      System.exit( ERROR_PARSING_ARGS_EXIT_CODE );
      return;
    }

    try
    {
      shade( c_input, c_output, c_relocations );
    }
    catch ( final Throwable t )
    {
      c_logger.log( Level.SEVERE, "Error: Error shading jar: " + t );
      t.printStackTrace();
      System.exit( ERROR_OTHER_EXIT_CODE );
      return;
    }
    System.exit( SUCCESS_EXIT_CODE );
  }

  private static void setupLogger()
  {
    c_logger.setUseParentHandlers( false );
    final ConsoleHandler handler = new ConsoleHandler();
    handler.setFormatter( new RawFormatter() );
    c_logger.addHandler( handler );
  }

  private static boolean processOptions( final String[] args )
  {
    // Parse the arguments
    final CLArgsParser parser = new CLArgsParser( args, OPTIONS );

    //Make sure that there was no errors parsing arguments
    if ( null != parser.getErrorString() )
    {
      c_logger.log( Level.SEVERE, "Error: " + parser.getErrorString() );
      return false;
    }

    // Get a list of parsed options
    final List<CLOption> options = parser.getArguments();
    for ( final CLOption option : options )
    {
      switch ( option.getId() )
      {
        case CLOption.TEXT_ARGUMENT:
        {
          c_logger.log( Level.SEVERE, "Error: Unexpected argument: " + option.getArgument() );
          return false;
        }
        case INPUT_OPT:
        {
          c_input = new File( option.getArgument() );
          break;
        }
        case OUTPUT_OPT:
        {
          c_output = new File( option.getArgument() );
          break;
        }

        case RELOCATION_OPT:
        {
          c_relocations.put( option.getArgument(), option.getArgument( 1 ) );
          break;
        }
        case VERBOSE_OPT:
        {
          c_logger.setLevel( Level.ALL );
          break;
        }
        case QUIET_OPT:
        {
          c_logger.setLevel( Level.WARNING );
          break;
        }
        case HELP_OPT:
        {
          printUsage();
          return false;
        }
      }
    }
    if ( null == c_input )
    {
      c_logger.log( Level.SEVERE, "Error: Input jar must be specified" );
      return false;
    }
    else if ( !c_input.exists() )
    {
      c_logger.log( Level.SEVERE, "Error: Input jar " + c_input + " must exist" );
      return false;
    }
    else if ( null == c_output )
    {
      c_logger.log( Level.SEVERE, "Error: Output jar must be specified" );
      return false;
    }
    else if ( c_relocations.isEmpty() )
    {
      c_logger.log( Level.SEVERE, "Error: Relocations must be specified" );
      return false;
    }
    else
    {
      if ( c_logger.isLoggable( Level.FINE ) )
      {
        c_logger.log( Level.INFO, "Input: " + c_input );
        c_logger.log( Level.INFO, "Output: " + c_output );
        for ( final Map.Entry<String, String> entry : c_relocations.entrySet() )
        {
          c_logger.log( Level.INFO, "  Relocation: " + entry.getKey() + " => " + entry.getValue() );
        }
      }

      return true;
    }
  }

  /**
   * Print out a usage statement
   */
  @SuppressWarnings( "StringBufferReplaceableByString" )
  private static void printUsage()
  {
    final String lineSeparator = System.getProperty( "line.separator" );

    final StringBuilder msg = new StringBuilder();

    msg.append( "java " );
    msg.append( Main.class.getName() );
    msg.append( " [options]" );
    msg.append( lineSeparator );
    msg.append( "Options: " );
    msg.append( lineSeparator );

    msg.append( CLUtil.describeOptions( OPTIONS ).toString() );

    c_logger.log( Level.INFO, msg.toString() );
  }

  public static void shade( final File source, final File target, final Map<String, String> relocations )
    throws Exception
  {
    final List<Relocator> relocators = relocations
      .entrySet()
      .stream()
      .map( r -> new SimpleRelocator( r.getKey(), r.getValue(), null, null, false ) )
      .collect( Collectors.toList() );
    final ShadeRequest request = new ShadeRequest();
    request.setFilters( Collections.emptyList() );
    request.setJars( Collections.singleton( source ) );
    request.setRelocators( relocators );
    request.setResourceTransformers( Collections.emptyList() );
    request.setUberJar( target );
    new DefaultShader().shade( request );
  }
}
