package procheck.dev.enrich.dao;

import java.sql.Connection;
import java.sql.DriverManager;
/**
 * Objet qui gere les connxion à la base de données
 * @author K.ABIDA
 *
 */
public class ConnManager {
	
	/**
	 * Cnx à la base
	 */
	private static Connection connection;
	
	/**
	 * se connecte à la base et retourne un objet de connexion
	 * @param url data source 
	 * @param username utilisateur DB
	 * @param password password DB
	 * @param drive le pilote à utiliser
	 * @return une connexion à la base de données
	 */
	public  static Connection ConnetionDB(String url,String username,String password,String drive) {
		try {
				Class.forName(drive);
				connection=DriverManager.getConnection(url,username,password);
		} catch (Exception e) {
				e.printStackTrace();
		}
		return connection;
		
	}

	public static Connection getConnection() {
		return connection;
	} 
	
}