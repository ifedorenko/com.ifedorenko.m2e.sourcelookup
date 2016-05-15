package decentxml.app.test;

import org.junit.Test;

import decentxml.app.Application;
import decentxml.app.fragment.Fragment;

public class ApplicationTest {

	@Test
	public void test() throws Exception {
		new Fragment().newDocument();
		new Application().start(null);
	}
}
