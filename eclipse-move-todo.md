sourcelookup


jdt.launching
* NEVER packages sources and shaded ASM sources (of not too difficult) in javaagent jar
* replace Guava with custom hashing and caching implementation
* UI to show source lookup information
* DONE javadoc 
* extensions documentation
* testing (would really appreciate help here!)
* command line build org.eclipse.jdt.launching bundle packaging
  * tycho
  * ant?
* java app launchDelegate
* DONE general code grooming
* decide if RemoteJavaAdvancedApplicationLauncher should be moved next to and/or reconciled with JavaRemoteApplicationLaunchConfigurationDelegate
* DONE AdvancedSourceLookup facade API to access functionality from internal classes
* API to discover and download 
* DONE WorkspaceProjectSourceContainers.addJavaProject is long running due to file hashing, add IProgressMonitor


pde
* move the code
* decide what to do with current pde source lookup implementation
  * keep as is, which will make it separate from advanced source lookup imple
  * refactor to use ISourceLookupParticipant API, which make it complimentary to the new code
* equinox hook packaging, likely requires changes to jdt.launching javaagent packaging
* p2 repository support
* LaunchDelegateImpl #toLocalFile likely has existing replacement


m2e
* move the code
* kill launch extensions hooks
* replace JDIHelpers and AdvancedSourceLookupSupport with AdvancedSourceLookup
