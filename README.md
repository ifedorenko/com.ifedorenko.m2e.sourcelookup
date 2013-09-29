# Overview and mode of operation

Allows Eclipse Java debugger lookup sources of Java classes dynamically loaded
by Maven, such as Maven plugins. Supports multiple versions of the same class
loaded by the Maven VM. Sources are looked up among Eclipse workspace projects
or resolved from Maven repositories as necessary.

Implementation uses -javaagent to instrument classes loaded by the target JVM
with informtation about actual classes folder or jar file the classes are
being loaded from. This information is captured in new JSR-45 SMAP stratum and
retrived by Eclipse JDT debugger over JDWP. 

The class folders and jar files are scanned for 
META-INF/maven/**/pom.properties to identify m2e project location and Maven 
artifact (groupId,artifactId,version) tuple or "GAV" for short. Enabled 
Maven (Nexus) indexes are also consulted to identify jar file GAV.  

The search for sources is performed in the following order
* m2e workspace project name and location, if present, are matched to existing
  workspace projects
* artifact GAV is matched to existing workspace projects
* sources jar artifact corresponding to the GAV is resolved from Maven
  repositories.

In addition to Maven-specific source code lookup, implementation also supports
Equinox-based runtimes, which is particularly useful for debugging Tycho builds
where Equinox is embedded in Maven runtime.

# Installation

* Install Eclipse SDK 4.3 M5 or better from http://download.eclipse.org/eclipse/downloads/ 
* Install m2e 1.3 or better from http://www.eclipse.org/m2e/download/
* Install Dynamic Sources Lookup m2e extension from 
  http://ifedorenko.github.com/m2e-extras/

# Use

Dynamic Sources Lookup m2e extension does not require any special configuration
and is enabled by default for all debug Maven launches.

# New in version 1.1.0.201309291737
* Maven 3.1.x and Tesla 3.1.x compatibility

# New in version 1.1

* Requires m2e 1.3 and Eclipse 4.3 (Kepler) M5
* [new] Maven (Nexus) indexes are now used to identify jar files GAV.
* [new] Ability to import artifacts as "Maven Binary" projects. Binary projects
  allow conditional breakpoints and other operations that require java project.
* [fix] Source editor should more reliably refresh after sources jar download.
* [fix] Exception stack traces do not show source file names  


# Known limitations

* Requires Java 5 or later but only tested with Java 6. Will not work and will
  likely cause JVM startup failure for Java 1.4 and earlier. As a 
  workaround, disable "Dynamic Sources Lookup" Maven Launch Extension in the 
  launch configuration dialog.
* Sources resolution uses repositories and pluginRepositories configured in
  Maven User settings.xml but not the actual repository or snapshot repository
  from <distributionManagement> the artifact the class was loaded from.
* Only locally running JVMs are supported. It is not possible to enable dynamic
  sources lookup for debug sessions connected to remotely running JVMs.
* javaagent implementation will not instrument classes that have existing
  JSR-45 SMAP.
