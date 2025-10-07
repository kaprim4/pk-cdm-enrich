package procheck.dev.enrich.tools;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.Locale;

import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageTypeSpecifier;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.metadata.IIOInvalidTreeException;
import javax.imageio.metadata.IIOMetadata;
import javax.imageio.stream.ImageOutputStream;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Element;
import com.github.jaiimageio.plugins.tiff.BaselineTIFFTagSet;
import com.github.jaiimageio.plugins.tiff.TIFFDirectory;
import com.github.jaiimageio.plugins.tiff.TIFFField;
import com.github.jaiimageio.plugins.tiff.TIFFImageWriteParam;
import com.github.jaiimageio.plugins.tiff.TIFFTag;

import procheck.dev.enrich.CommonVars;
import procheck.dev.enrich.Main;

/**
 * Des outils pour la manipulation des images
 * 
 * @author K.ABIDA
 *
 */
public class ImageTools {

	/**
	 * Insere les donn�es remise dans une images ticket remises (pour les remises
	 * virtuelles).
	 * 
	 * @param image      l'image remise vierge
	 * @param agence     l'agence remettante
	 * @param compte     le numero de compte
	 * @param reference  la reference remise
	 * @param nbrValeur  le nombre de valeurs dans la remise
	 * @param montant    le montant de la remise
	 * @param dateRemise la date de la remise
	 * @param typeRemise type de la remise
	 * @param nomCompte  le nom du client
	 * @return true si ok false si non
	 */
	public static boolean putAllDataInImageRemise( BufferedImage image, String agence, String compte, String reference,
		String nbrValeur, String montant, String dateRemise, String referenceClient, String nomCompte) {
		int boucle = 0;
		boucle++;
		CommonVars.logger.info("ImageTools.putAllDataInImageRemise START ["+boucle+"");
		CommonVars.logger.info("_________________________________________");
		CommonVars.logger.info("agence : "+agence);
		CommonVars.logger.info("compte : "+compte);
		CommonVars.logger.info("reference : "+reference);
		CommonVars.logger.info("nbrValeur : "+nbrValeur);
		CommonVars.logger.info("montant : "+montant);
		CommonVars.logger.info("dateRemise : "+dateRemise);
		CommonVars.logger.info("referenceClient : "+referenceClient);
		CommonVars.logger.info("nomCompte : "+nomCompte);
		CommonVars.logger.info("_________________________________________");
		boolean isOk = true;
		Graphics g = null;
		try {
				g = image.getGraphics();
				g.setFont(new Font("default", Font.BOLD, 35));
				g.setColor(new Color(0, 0, 0));
				String configRemise[] = Main.REMISE_CONFIG_FOR_VIRTUAL.split(";");
				isOk = putOneDataInImage(g, agence, configRemise[0]);
				if (isOk) {
					isOk = putOneDataInImage(g, compte, configRemise[1]);
				}
				if (isOk) {
					isOk = putOneDataInImage(g, reference, configRemise[2]);
				}
				if (isOk) {
					isOk = putOneDataInImage(g, nbrValeur, configRemise[3]);
				}
				if (isOk) {
					isOk = putOneDataInImage(g, montant, configRemise[4]);
				}
				if (isOk) {
					isOk = putOneDataInImage(g, dateRemise, configRemise[5]);
				}
				if (isOk) {
					isOk = putOneDataInImage(g, referenceClient, configRemise[6]);
				}
				if (isOk) {
					isOk = putOneDataInImage(g, nomCompte, configRemise[7]);
				}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				g.dispose();
			} catch (Exception e2) {
				// TODO: handle exception
			}
		}
		return isOk;
	}

	/**
	 * Insere les donn�es lot dans une images ticket remises (pour les lot
	 * virtuels).
	 * 
	 * @param image  l'image lot vierge
	 * @param agence l'agence remettante
	 * @param Date   la date du lot
	 * @return true si ok false si non
	 */
	public static boolean putAllDataInImageLot(BufferedImage image, String agence, String Date) {
		boolean isOk = true;
		Graphics g = null;
		try {
			g = image.getGraphics();
			g.setFont(new Font("default", Font.BOLD, 35));
			g.setColor(new Color(0, 0, 0));
			String configLot[] = Main.LOT_CONFIG_FOR_VIRTUAL.split(";");
			isOk = putOneDataInImage(g, agence, configLot[0]);
			if (isOk) {
				isOk = putOneDataInImage(g, Date, configLot[1]);
			}
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			try {
				g.dispose();
			} catch (Exception e2) {
				// TODO: handle exception
			}
		}
		return isOk;
	}

	/**
	 * insert une donn�e dans une image
	 * 
	 * @param g      l'image vierge
	 * @param data   la donn�e � inserer
	 * @param config la configuration des position
	 * @return true si ok false si non
	 */
	public static boolean putOneDataInImage(Graphics g, String data, String config) {
		boolean isOk = true;
		try {
			String cfg[] = config.split(",");
			int x = Integer.parseInt(cfg[0]);
			int y = Integer.parseInt(cfg[1]);
			if (x >= 0 && y >= 0 && data != null) {
				g.drawString(data, x, y);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return isOk;
	}

	/**
	 * conversion de l'image en auto closable
	 * 
	 * @param image l'image en question
	 * @param type  le type de l'image
	 * @return la nouvelle image en byte
	 * @throws Exception l'exception si gener�e
	 */
//	public static byte[] toByteArrayAutoClosable(BufferedImage image, String type) throws Exception {
//	    String debugFolderPath = "D:\\CDM\\PKEnrichi\\LCN\\Remise\\"; // Specify the folder where you want to save the image for debugging
//
//	    try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
//	        ImageIO.write(image, type, out);
//	        byte[] imageData = out.toByteArray();
//	        
//	        // Generate timestamp
//	        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
//	        String timestamp = dateFormat.format(new Date());
//	        
//	        // Save the image to the debug folder with timestamp
//	        String debugFilePath = debugFolderPath + "debug_image_" + timestamp + "." + type;
//	        File debugFile = new File(debugFilePath);
//	        ImageIO.write(image, type, debugFile);
//
//	        return imageData;
//	    }
//	}

	public static byte[] toByteArrayAutoClosable(BufferedImage image, String type) throws Exception {
		try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
			ImageIO.write(image, type, out);
			return out.toByteArray();
		}
	}
	

	/**
	 * Compresse une image en format tiff "CCITT T.6"
	 * 
	 * @param image l'image en question
	 * @return l'image en byte
	 */
	public static byte[] compressTiff(BufferedImage image) {
		ImageOutputStream ios = null;
		ImageWriter writer = null;
		try {
			Iterator<ImageWriter> it = ImageIO.getImageWritersByFormatName("TIF");
			if (it.hasNext()) {
				writer = (ImageWriter) it.next();
			} else {
				return null;
			}
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ios = ImageIO.createImageOutputStream(baos);
			writer.setOutput(ios);

			TIFFImageWriteParam writeParam = new TIFFImageWriteParam(Locale.ENGLISH);
			writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
			writeParam.setCompressionType("CCITT T.6");
			IIOMetadata data = writer.getDefaultImageMetadata(new ImageTypeSpecifier(image), writeParam);
			IIOMetadata dataNew = setDPIViaAPI(data);
			IIOImage iioImage = new IIOImage(image, null, dataNew);
			writer.write(dataNew, iioImage, writeParam);
			ios.close();
			writer.dispose();
			return baos.toByteArray();
		} catch (Exception e) {
			CommonVars.logger.error("PKException#compressTiff#", e);
			e.printStackTrace();
			return null;
		}
	}
	
	public static byte[] compressJpg(BufferedImage image) {
		try {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			ImageIO.write(image, "jpg", baos);
			return baos.toByteArray();
		} catch (Exception e) {
			CommonVars.logger.error("PKException#compressTiff#", e);
			e.printStackTrace();
			return null;
		}
	}


	/**
	 * Change la resolution DPI d'une image donn�e
	 * 
	 * @param imageMetadata les donn�es de parametrage de l'image en question
	 * @return le nouveau imageMetadata apres modification de la resolution DPI
	 * @throws IIOInvalidTreeException l'exception si gener�e
	 */
	private static IIOMetadata setDPIViaAPI(IIOMetadata imageMetadata) throws IIOInvalidTreeException {
		// Derive the TIFFDirectory from the metadata.
		TIFFDirectory dir = TIFFDirectory.createFromMetadata(imageMetadata);

		// Get {X,Y}Resolution tags.
		BaselineTIFFTagSet base = BaselineTIFFTagSet.getInstance();
		TIFFTag tagXRes = base.getTag(BaselineTIFFTagSet.TAG_X_RESOLUTION);
		TIFFTag tagYRes = base.getTag(BaselineTIFFTagSet.TAG_Y_RESOLUTION);
		TIFFTag tagResolutionUnit = base.getTag(BaselineTIFFTagSet.TAG_RESOLUTION_UNIT);

		// TIFFTag tagXWidth = base.getTag(BaselineTIFFTagSet.TAG_IMAGE_WIDTH);

		// TIFFTag tagRowsPerStrip = base.getTag(BaselineTIFFTagSet.TAG_ROWS_PER_STRIP);
		// TIFFField fieldRowsPerStrip = new TIFFField(tagRowsPerStrip,
		// TIFFTag.TIFF_SHORT, 1, (Object)new char[]{2200});

		// Create {X,Y}Resolution fields.
		TIFFField fieldXRes = new TIFFField(tagXRes, TIFFTag.TIFF_RATIONAL, 1, new long[][] { { 200, 1 } });
		TIFFField fieldYRes = new TIFFField(tagYRes, TIFFTag.TIFF_RATIONAL, 1, new long[][] { { 200, 1 } });
		TIFFField fieldResolutionUnit = new TIFFField(tagResolutionUnit, TIFFTag.TIFF_RATIONAL, 1,
				new long[][] { { 2, 1 } });

		// TIFFField fieldXWidth = new TIFFField(tagXWidth, TIFFTag.TIFF_RATIONAL,
		// 1, new long[][] {{800, 1}});
		// Append {X,Y}Resolution fields to directory.

		// dir.addTIFFField(fieldRowsPerStrip);

		dir.removeTIFFField(BaselineTIFFTagSet.TAG_X_RESOLUTION);
		dir.removeTIFFField(BaselineTIFFTagSet.TAG_Y_RESOLUTION);
		dir.removeTIFFField(BaselineTIFFTagSet.TAG_RESOLUTION_UNIT);
		dir.addTIFFField(fieldXRes);
		dir.addTIFFField(fieldYRes);
		dir.addTIFFField(fieldResolutionUnit);
		// dir.addTIFFField(fieldXWidth);

		// Convert to metadata object and return.
		return dir.getAsMetadata();
	}

	/**
	 * list les attribut d'un element XML
	 * 
	 * @param element l'elemeny xml
	 */
	public static void listAllAttributes(Element element) {

		TransformerFactory transformerFactory = TransformerFactory.newInstance();
		Transformer transformer = null;
		try {
			transformer = transformerFactory.newTransformer();
		} catch (TransformerConfigurationException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		DOMSource source = new DOMSource(element);
		StreamResult result = new StreamResult(new StringWriter());

		try {
			transformer.transform(source, result);
		} catch (TransformerException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		System.out.println(result.getWriter().toString());
	}
}
