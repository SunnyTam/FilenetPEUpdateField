import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Properties;
import java.util.Set;

import com.filenet.api.constants.RefreshMode;
import com.filenet.api.core.Connection;
import com.filenet.api.core.Document;
import com.filenet.api.core.Domain;
import com.filenet.api.core.Factory;
import com.filenet.api.core.Folder;
import com.filenet.api.core.ObjectStore;
import com.filenet.api.util.UserContext;

import filenet.vw.api.VWFetchType;
import filenet.vw.api.VWRoster;
import filenet.vw.api.VWRosterQuery;
import filenet.vw.api.VWSession;
import filenet.vw.api.VWWorkObject;


public class Main {
	
	public static String P8_URL;
	public static String USER_ID;
	public static String PASSWORD;
	public static String CONN_PT;
	public static String ROSTER_NAME;
	public static String FIELD_NAME;
	public static String OS;
	public static HashMap<String,String> map;
	
	public static void readConfig() throws FileNotFoundException, IOException{
		System.out.println("reading config.properties ...");
		Properties p = new Properties();
		p.load(new FileInputStream("config.properties"));
		P8_URL = p.getProperty("P8_URL");
		USER_ID = p.getProperty("USER_ID");
		PASSWORD = p.getProperty("PASSWORD");
		CONN_PT =  p.getProperty("CONN_PT");
		ROSTER_NAME = p.getProperty("ROSTER_NAME");
		FIELD_NAME = p.getProperty("FIELD_NAME");
		OS = p.getProperty("OS");
	}
	
	public static void readMapping() throws FileNotFoundException, IOException{
		Properties prop = new Properties();
		prop.load(new FileInputStream("mapping.properties"));
		map = new HashMap(prop);
	}
	
	public static VWSession getVWSession() {
		VWSession session = new VWSession();
		session.setBootstrapCEURI(P8_URL);
		session.logon(USER_ID, PASSWORD, CONN_PT);
		return session;
	}
	
	public static String getMapValue(String oldVal){
		String newVal = oldVal;
		if(map.containsKey(oldVal)){
			newVal = map.get(oldVal);
		}
		return newVal;
	}
	
	public static Domain getDomain(){
		Connection conn = Factory.Connection.getConnection(P8_URL);
		UserContext.get().pushSubject(UserContext.createSubject(conn,USER_ID, PASSWORD, null));
		return Factory.Domain.fetchInstance(conn, null, null);
	}
	
	public static void main(String[] args) {
		try {
			readConfig();
			readMapping();
		}
		catch (Exception e) {
			e.printStackTrace();
			return;
		}

		
		VWSession session = getVWSession();
		VWRosterQuery query = session.getRoster(ROSTER_NAME).createQuery(null, null, null, VWRoster.QUERY_NO_OPTIONS, null, null, VWFetchType.FETCH_TYPE_WORKOBJECT);
		int count = 0;
		while (query.hasNext()){
			VWWorkObject workObj = (VWWorkObject) query.next();
			System.out.println();
			System.out.println("count: " + ++count);
			System.out.println("id: " + workObj.getWorkObjectNumber());
			if (!workObj.hasFieldName(FIELD_NAME)) {
				System.out.println("no " + FIELD_NAME + " field found!");
				continue;
			}
			String oldFieldValue = workObj.getDataField(FIELD_NAME).toString();
			String newFieldValue = getMapValue(oldFieldValue);
			if(!oldFieldValue.equals(newFieldValue)){
				System.out.println("old field: " + oldFieldValue);
				System.out.println("updating field: " + newFieldValue);
				workObj.doLock(true);
				workObj.setFieldValue(FIELD_NAME, newFieldValue, false);
				workObj.doSave(true);
			}
			System.out.println("success! get: " + workObj.getDataField(FIELD_NAME).toString());
		}
		System.out.println();
		System.out.println("done.");
		
	}
}
