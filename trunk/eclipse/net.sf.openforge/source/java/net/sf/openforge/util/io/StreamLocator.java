package net.sf.openforge.util.io;

import java.io.InputStream;

public interface StreamLocator {
	
	InputStream  getAsStream(String name) ;
}
