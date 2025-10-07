package procheck.dev.enrich.threads.gab;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.lang3.StringUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import procheck.dev.enrich.CommonVars;

/**
 * Objet qui contient les donn�es remise pour le traitement GAB
 * 
 * @author N.BENZAIRA
 *
 */
class RemiseData {
	/**
	 * Code d'agence du compte remettant
	 */
	String agenceCode;
	/**
	 * la Date de la remise
	 */
	String dateOp;
	
	/**
	 * code du GAB
	 */
	String atmCode;
	/**
	 * Compte remettant
	 */
	String compteR;
	
	/**
	 * Reference de la remise
	 */
	String refRemise;
	
	/**
	 * Montant de la remise
	 */
	String remAmount;
	
	/**
	 * Liste des valeurs dans la remise
	 */
	List<ChequeData> chequesLst;

	public RemiseData() {
		chequesLst = new ArrayList<ChequeData>();
	}

	/**
	 * Chargement des donn�es de la remise
	 * @param xmlFile le fichier xml dans le zip GAB
	 * @param zipFile le zip GAB
	 * @param listFileImage liste des images des valeurs dans le Zip GAB
	 * @return true si ok false sinon
	 */
	public boolean loadDataRemise(InputStream stream, List<ZipEntry> listFileImage,ZipFile imgZipFile) { // ,String nameImg
		boolean isOk = true;
		try {
			CommonVars.logger.info("Start RemiseData.loadDataRemise");
			//InputStream stream = zipFile.getInputStream(xmlFile);
			StringBuffer sb = new StringBuffer();
			BufferedReader br = null;
			br = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8));
			String line = null;
			br.readLine();
			while ((line = br.readLine()) != null) {
				sb.append(line);
			}
			String cleanedXml = sb.toString().replaceAll("^.*<\\?xml", "<?xml");
			Document dc = convertStringToXMLDocument(cleanedXml);
			NodeList nl = dc.getElementsByTagName("document");
			int idx = 0;
			int order =1;
			for (idx = 0; idx < nl.getLength(); idx++) {
				Element ele = (Element) nl.item(idx);
				if (ele.getElementsByTagName("type").item(0).getTextContent().equalsIgnoreCase("REMITTANCE")) {
					//this.agenceCode = ele.getElementsByTagName("Agence").item(0).getTextContent();
			        NodeList agenceNodes = ele.getElementsByTagName("agence");
			        Node agenceNode = agenceNodes.item(0);
		            if (agenceNode.getNodeType() == Node.ELEMENT_NODE) {
		                Element agenceElement = (Element) agenceNode;
		                this.agenceCode = agenceElement.getAttribute("code");
		            }
					this.atmCode = "";
					this.dateOp = ele.getElementsByTagName("dateValeur").item(0).getTextContent();
					this.dateOp = this.dateOp.replace("/", "");
					String cmc7=ele.getElementsByTagName("cmc7").item(0).getTextContent();
					this.compteR = cmc7.substring(cmc7.indexOf('>')+1,cmc7.indexOf('>')+13);
					//this.compteR = cmc7.substring(cmc7.indexOf(';')+1,cmc7.indexOf(':')+13);
					System.out.println("compteR : " + compteR);
					this.refRemise = cmc7.substring(1,8);
					this.remAmount = ele.getElementsByTagName("montant").item(0).getTextContent();
				}else if(ele.getElementsByTagName("type").item(0).getTextContent().equalsIgnoreCase("CHEQUE")) {
					String cmc7 = ele.getElementsByTagName("cmc7").item(0).getTextContent().replace(":", "").replaceAll("[><]", ";");
					String id = ele.getAttribute("id");
					String img = StringUtils.leftPad(id, 8,'0'); // nameImg.indexOf('.') != -1 ? nameImg.split("\\.")[0] : nameImg;
					ChequeData chequeData = new ChequeData(cmc7, img, order+"");
					isOk = chequeData.putImagesCheque(listFileImage, imgZipFile);
					order++;
					if (!isOk) {
						break;
					}
					this.chequesLst.add(chequeData);
				}
				
			}
		} catch (Exception e) {
			CommonVars.logger.error("PCException #RemiseData.loadDataRemise#", e);
			isOk = false;
		}
		CommonVars.logger.info("End RemiseData.loadDataRemise[" + isOk + "]");
		return isOk;
	}

	/**
	 * converte une chaine de caractere � un document XML
	 * @param xmlString la chaine de caracteres en question
	 * @return un Document XML
	 */
	private static Document convertStringToXMLDocument(String xmlString) {
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		DocumentBuilder builder = null;
		try {
			builder = factory.newDocumentBuilder();
			Document doc = builder.parse(new InputSource(new StringReader(xmlString)));
			return doc;
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

}
