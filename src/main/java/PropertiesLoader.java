import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class PropertiesLoader extends Properties{
	private static final Logger LOGGER = LoggerFactory.getLogger(PropertiesLoader.class);

	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	Properties props = new Properties();
	public static String sevenZip = null;
	public static String urarDirectory = null;
	private InputStream input = null;

	private static PropertiesLoader instance;

	public static PropertiesLoader getInstance() throws Exception {
		if (instance == null) {
			instance = new PropertiesLoader();
		}
		return instance;
	}

	public Properties initProperties() {
		input = Watcher.class.getClassLoader().getResourceAsStream("config.properties");

		// load a properties file
		try {
			props.load(input);
		} catch (IOException e1) {
			LOGGER.error("Unable to load properties " + e1);
		}
		//PropertiesLoader.setSevenZip(props.getProperty("sevenzip"));
		props.setProperty("sevenzip", props.getProperty("sevenzip"));
		props.setProperty("unrardirectory", props.getProperty("unrar.destination"));
		//PropertiesLoader.setUrarDirectory(props.getProperty("unrar.destination"));
		if (input != null) {
			try {
				input.close();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return props;
	}

	public String getSevenZip() {
		return sevenZip;
	}

	private static void setSevenZip(String sevenZip) {
		PropertiesLoader.sevenZip = sevenZip;
	}

	public String getUrarDirectory() {
		return urarDirectory;
	}

	private static void setUrarDirectory(String urarDirectory) {
		PropertiesLoader.urarDirectory = urarDirectory;
	}
}
