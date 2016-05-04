package decentxml.app;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;

import de.pdark.decentxml.Document;
import de.pdark.decentxml.Element;

public class Application implements IApplication {

  @Override
  public Object start(IApplicationContext context) throws Exception {
    Document doc = new Document();
    doc.setRootNode(new Element("test"));
    System.out.println(doc.toXML());
    return doc.toString();
  }

  @Override
  public void stop() {}

}
