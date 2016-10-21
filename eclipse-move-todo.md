sourcelookup


jdt.launching
* testing (would really appreciate help here!)
* UI to show source lookup information
* command line build org.eclipse.jdt.launching bundle packaging
  * tycho
  * ant?
* DONE replace Guava with custom hashing, caching, collections implementation
  * DONE remove project locations from project dependencies in JavaProjectDescription (better binary project support)
* DONE different file hash caching config for bulk workspace indexing and for source lookup (unbounded for workspace indexing, lru for source lookup)
* DONE extensions documentation
* NEVER packages sources and shaded ASM sources (of not too difficult) in javaagent jar
* DONE javadoc 
* DONE java app launchDelegate
* DONE general code grooming
* DONE decide if RemoteJavaAdvancedApplicationLauncher should be moved next to and/or reconciled with JavaRemoteApplicationLaunchConfigurationDelegate
* DONE AdvancedSourceLookup facade API to access functionality from internal classes
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
* DONE replace JDIHelpers and AdvancedSourceLookupSupport with AdvancedSourceLookup
