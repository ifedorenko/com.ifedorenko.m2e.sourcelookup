package com.ifedorenko.m2e.sourcelookup.internal.launch;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.debug.core.sourcelookup.ISourceLookupDirector;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.project.IMavenProjectChangedListener;
import org.eclipse.m2e.core.project.MavenProjectChangedEvent;

import com.ifedorenko.m2e.sourcelookup.internal.jdt.SourceLookupParticipant;

public class MavenSourceLookupParticipant extends SourceLookupParticipant implements IMavenProjectChangedListener {

  @Override
  public void init(ISourceLookupDirector director) {
    super.init(director);
    MavenPlugin.getMavenProjectRegistry().addMavenProjectChangedListener(this);
  }

  @Override
  public void dispose() {
    MavenPlugin.getMavenProjectRegistry().removeMavenProjectChangedListener(this);
    super.dispose();
  }

  @Override
  public void mavenProjectChanged(MavenProjectChangedEvent[] events, IProgressMonitor monitor) {
    disposeContainers();
  }
}
