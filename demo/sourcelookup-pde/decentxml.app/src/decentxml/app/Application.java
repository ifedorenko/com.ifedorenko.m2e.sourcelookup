package decentxml.app;

import java.util.concurrent.Callable;

import org.eclipse.equinox.app.IApplication;
import org.eclipse.equinox.app.IApplicationContext;

import de.pdark.decentxml.Document;
import de.pdark.decentxml.Element;

public class Application implements IApplication {

  @Override
  public Object start(IApplicationContext context) throws Exception {
    getFragmentClass().call();
    Document doc = new Document();
    doc.setRootNode(new Element("test"));
    System.out.println(doc.toXML());
    return doc.toString();
  }

  @SuppressWarnings("unchecked")
  private Callable<Document> getFragmentClass() throws Exception {
    ClassLoader cl = getClass().getClassLoader();
    Class<?> c = cl.loadClass("decentxml.app.fragment.Fragment");
    return (Callable<Document>) c.newInstance();
  }

  @Override
  public void stop() {}

}
