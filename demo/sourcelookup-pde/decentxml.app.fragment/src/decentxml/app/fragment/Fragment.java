package decentxml.app.fragment;

import java.util.concurrent.Callable;

import de.pdark.decentxml.Document;

public class Fragment implements Callable<Document> {

  public Document newDocument() {
    return new Document();
  }

  @Override
  public Document call() throws Exception {
    return newDocument();
  }
}
