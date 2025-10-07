package procheck.dev.enrich.cmc7;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
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
import com.github.jaiimageio.plugins.tiff.BaselineTIFFTagSet;
import com.github.jaiimageio.plugins.tiff.TIFFDirectory;
import com.github.jaiimageio.plugins.tiff.TIFFField;
import com.github.jaiimageio.plugins.tiff.TIFFImageWriteParam;
import com.github.jaiimageio.plugins.tiff.TIFFTag;

import procheck.dev.enrich.CommonVars;
import procheck.dev.enrich.threads.PCDocument;
import procheck.dev.lib.PCHStringOpr;
import procheck.dev.lib.ai.PKOCRCMC7;
import procheck.dev.lib.imageworker.PKImageLib;

/**
 * Objet pour utilisation des services CMC7
 * 
 * @author K.ABIDA
 *
 */
public class Cmc7Work {

	/**
	 * Encodage
	 */
	public static String CHAR_ENCODING = "ISO-8859-1";

	public Cmc7Work() {
	}

	/**
	 * Recupere le crop en BW qui contient le CMC7 pour une image donn�e
	 * 
	 * @param document l'objet qui contient toute les informations sur une valeur
	 * @param dpi      la resolution DPI
	 * @return le crop en bytes
	 * @throws Exception l'exception gener�e
	 */
	private static byte[] getCropCMC7Tiff(PCDocument document, int dpi) throws Exception {
		if(document.getImageBWFront() == null)
			CommonVars.logger.warn("Document has no image BW to compress");
		
		System.out.println("IS : "+document.getImageBWFront()+ "/ DPI : "+dpi);
		InputStream in = compressTiff(document.getImageBWFront(), dpi) == null 
			    ? new ByteArrayInputStream(new byte[0])  // Flux vide en cas de null
			    : new ByteArrayInputStream(compressTiff(document.getImageBWFront(), dpi));

		BufferedImage originalImage = ImageIO.read(in);
		if(originalImage == null)
			CommonVars.logger.warn("originalImage IS NULL ");
		else {
			CommonVars.logger.info("X : "+originalImage.getWidth() +"/"+ CommonVars.srvConf.getCheque().getCmc7().getX());
			CommonVars.logger.info("Y : "+originalImage.getHeight() +"/"+ CommonVars.srvConf.getCheque().getCmc7().getY());
			CommonVars.logger.info("W : "+originalImage.getWidth() +"/"+ CommonVars.srvConf.getCheque().getCmc7().getW());
			CommonVars.logger.info("H : "+originalImage.getHeight() +"/"+ CommonVars.srvConf.getCheque().getCmc7().getH());	
		}
		
		
		BufferedImage croppedImage = originalImage.getSubimage(
				(int) (originalImage.getWidth() * CommonVars.srvConf.getCheque().getCmc7().getX()),
				(int) (originalImage.getHeight() * CommonVars.srvConf.getCheque().getCmc7().getY()),
				(int) (originalImage.getWidth() * CommonVars.srvConf.getCheque().getCmc7().getW()),
				(int) (originalImage.getHeight() * CommonVars.srvConf.getCheque().getCmc7().getH()));
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ImageIO.write(croppedImage, "BMP", baos);
		baos.flush();
		return compressTiff(baos.toByteArray(), dpi);
	}

	/**
	 * Recupere le crop en TG qui contient le CMC7 pour une image donn�e
	 * 
	 * @param document l'objet qui contient toute les informations sur une valeur
	 * @param dpi      la resolution DPI
	 * @return le crop en bytes
	 * @throws Exception l'exception gener�e
	 */
	private static byte[] getCropCMC7TGR(PCDocument document, int dpi,boolean rotateImage,boolean sendVerso) throws Exception {
		InputStream in;
		if(rotateImage)
		{
			in = new ByteArrayInputStream(PKImageLib.rotateImage(document.getImageColorFront(), 180));
		}else if(sendVerso) {
			in = new ByteArrayInputStream(document.getImageColorRear());
		}else {
			in = new ByteArrayInputStream(document.getImageColorFront());
		}
		 
		BufferedImage originalImage = ImageIO.read(in);
		BufferedImage result = new BufferedImage(originalImage.getWidth(), originalImage.getHeight(),
				BufferedImage.TYPE_BYTE_GRAY);

		Graphics2D graphic = result.createGraphics();
		graphic.drawImage(originalImage, 0, 0, Color.WHITE, null);
		graphic.dispose();

		BufferedImage croppedImage = result.getSubimage(
				(int) (result.getWidth() * CommonVars.srvConf.getCheque().getCmc7().getX()),
				(int) (result.getHeight() * CommonVars.srvConf.getCheque().getCmc7().getY()),
				(int) (result.getWidth() * CommonVars.srvConf.getCheque().getCmc7().getW()),
				(int) (result.getHeight() * CommonVars.srvConf.getCheque().getCmc7().getH()));
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		ImageIO.write(croppedImage, "BMP", baos);
		baos.flush();
		return compressTiffTGR(baos.toByteArray(), dpi);
	}

	/**
	 * Compresse une image en Tiff
	 * 
	 * @param imageBytes l'image en question
	 * @param dpi        la resolution dpi
	 * @return une image compress�e format tiff
	 */
	public static byte[] compressTiffTGR(byte[] imageBytes, int dpi) {
		BufferedImage image;

		ImageOutputStream ios = null;
		ImageWriter writer = null;
		try {
			ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes);
			image = ImageIO.read(bais);
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
			writeParam.setCompressionType("LZW");
			IIOMetadata data = writer.getDefaultImageMetadata(new ImageTypeSpecifier(image), writeParam);
			IIOMetadata dataNew = setDPIViaAPI(data, dpi);
			IIOImage iioImage = new IIOImage(image, null, dataNew);
			writer.write(dataNew, iioImage, writeParam);
			ios.close();
			writer.dispose();
			return baos.toByteArray();
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	/**
	 * Recupere la valeur de CMC7 apres traitement serveur CMC7
	 * 
	 * @param document l'objet qui contient toute les informations sur une valeur
	 * @param isBW     true si blanc et noir false sinon
	 * @param dpi      resolution dpi
	 * @return la valeur de CMC7
	 */
	public static String getCMC7FromServer(PCDocument document, boolean isBW, int dpi,boolean rotateImage,boolean sendVerso) {
		CommonVars.logger.info("Cmc7Work.getCMC7FromServer START");
		String retValue = null;
		Socket sockClient = null;
		byte[] buff = new byte[128];
		try {
			byte[] cropCMC7;
			if (isBW) {
				cropCMC7 = getCropCMC7Tiff(document, dpi);
			} else {
				cropCMC7 = getCropCMC7TGR(document, dpi,rotateImage,sendVerso);
			}
			int lng = cropCMC7.length;
			String lngStr = PCHStringOpr.leftPad(lng + "", "0", 6);
			System.out.println("Sent lng = " + lngStr);
			byte[] msgToSend = new byte[6 + cropCMC7.length];
			System.arraycopy(lngStr.getBytes(), 0, msgToSend, 0, 6);
			System.arraycopy(cropCMC7, 0, msgToSend, 6, cropCMC7.length);

			sockClient = new Socket();
			sockClient.connect(
					new InetSocketAddress(CommonVars.srvConf.getIpCmc7Server(), CommonVars.srvConf.getPortCmc7Server()),
					10000);
			sockClient.setSoTimeout(10 * 1000);
			sockClient.getOutputStream().write(msgToSend);
			StringBuffer sb = new StringBuffer();
			int readLn;
			String msgRecData = null;
			while ((readLn = sockClient.getInputStream().read(buff)) != -1) {
				msgRecData = new String(buff, 0, readLn, CHAR_ENCODING);
				sb.append(msgRecData);
			}
			retValue = sb.toString();
			CommonVars.logger.info("Recieved CMC7 from server[" + retValue + "]");
			if (retValue != null) {
				if (retValue.startsWith("E-")) {
					CommonVars.logger.error("Stopping Thread, CMC7 Error .......");
					// procheck.dev.enrich.Main.STOP_ME = true;
				} else if (retValue.startsWith("OK-? ")) {
					retValue = retValue.substring(5);
				} else if (retValue.startsWith("OK-1 ")) {
					retValue = retValue.substring(5);
				} else if (retValue.startsWith("OK-?")) {
					retValue = retValue.substring(4);
				} else if (retValue.startsWith("OK-")) {
					retValue = retValue.substring(3);
				}
			}
		} catch (java.net.SocketTimeoutException e) {
			retValue = "PKException-Connect";
			//CommonVars.logger.error("#Cmc7Work.getCMC7FromServer# Timeout Exception", e);
			CommonVars.logger.error("#Cmc7Work.getCMC7FromServer# Timeout Exception");
			// CommonVars.logger.error("Stopping Thread .......");
			// procheck.dev.enrich.Main.STOP_ME = true;
		} catch (java.net.ConnectException e) {
			retValue = "PKException-Connect";
			// CommonVars.logger.error("#Cmc7Work.getCMC7FromServer# Connection Exception", e);
			CommonVars.logger.error("#Cmc7Work.getCMC7FromServer# Connection Exception");
			// CommonVars.logger.error("Stopping Thread .......");
			// procheck.dev.enrich.Main.STOP_ME = true;
		} catch (Exception ee) {
			CommonVars.logger.error("#Cmc7Work.getCMC7FromServer# Exception", ee);
			retValue = null;
		} finally {
			try {
				sockClient.close();
			} catch (Exception e) {
				// TODO: handle exception
				e.printStackTrace();			}
		}
		CommonVars.logger.info("Cmc7Work.getCMC7FromServer END");
		return retValue;
	}
	/**
	 * 
	 * @param image byte
	 * @param type  valeur CHQ/LCN
	 * @return cmc7 separer par H
	 */
	public static String getCMC7FromServerAI(byte[] image, String typeValeur) {
		String cmc7Ligne = null;
		try {
			PKOCRCMC7 pkOCRCMC7 = new PKOCRCMC7();
			boolean retOCR = pkOCRCMC7.doWork(CommonVars.OCR_AI_URL_VALEUR, image, typeValeur);
			CommonVars.logger.info("OCR CMC7 RETURN From AI: [" + retOCR + "]");
			CommonVars.logger.info("OCR CMC7 http status : " + pkOCRCMC7.getDataOCR().httpStatus);
			CommonVars.logger.info("OCR CMC7 http body : " + pkOCRCMC7.getDataOCR().dataReturn);
			if (!retOCR) {
				CommonVars.logger.error(pkOCRCMC7.getErrorCode());
			} else {
				cmc7Ligne = pkOCRCMC7.getDataOCR().cmc7;
			}
			CommonVars.logger.info("Recieved CMC7 from server AI[" + cmc7Ligne + "]");

		} catch (Exception ee) {
			CommonVars.logger.error("#Cmc7Work.getCMC7FromServerAI#", ee);
			cmc7Ligne = null;
		}
		return cmc7Ligne;
	}

	/**
	 * compress format tiff type CCITT
	 * 
	 * @param imageBytes l'image en question
	 * @param dpi        la resolution DPI
	 * @return l'image compress�e
	 */

public static byte[] compressTiff(byte[] imageBytes, int dpi) {
    BufferedImage image;
    ImageOutputStream ios = null;
    ImageWriter writer = null;
    try {
        // Read original image
        ByteArrayInputStream bais = new ByteArrayInputStream(imageBytes);
        BufferedImage originalImage = ImageIO.read(bais);

        // Convert to 1-bit (black & white)
        BufferedImage binaryImage = new BufferedImage(
            originalImage.getWidth(), originalImage.getHeight(),
            BufferedImage.TYPE_BYTE_BINARY
        );
        Graphics2D g2d = binaryImage.createGraphics();
        g2d.drawImage(originalImage, 0, 0, null);
        g2d.dispose();

        // Get TIFF writer
        Iterator<ImageWriter> it = ImageIO.getImageWritersByFormatName("TIFF");
        if (it.hasNext()) {
            writer = it.next();
        } else {
            return null;
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ios = ImageIO.createImageOutputStream(baos);
        writer.setOutput(ios);

        // Set compression parameters
        TIFFImageWriteParam writeParam = new TIFFImageWriteParam(Locale.ENGLISH);
        writeParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
        writeParam.setCompressionType("CCITT T.6");

        // Set DPI
        IIOMetadata data = writer.getDefaultImageMetadata(new ImageTypeSpecifier(binaryImage), writeParam);
        IIOMetadata dataNew = setDPIViaAPI(data, dpi);

        // Write TIFF
        IIOImage iioImage = new IIOImage(binaryImage, null, dataNew);
        writer.write(dataNew, iioImage, writeParam);

        // Clean up
        ios.close();
        writer.dispose();
        return baos.toByteArray();
    } catch (Exception e) {
        e.printStackTrace();
        return null;
    }
}

	/**
	 * changement de la resultion dpi
	 * 
	 * @param imageMetadata parametre d'image source
	 * @param dpi           la resolution dpi cible
	 * @return le nouveau parametrage
	 * @throws IIOInvalidTreeException l'exception gener�e
	 */
	private static IIOMetadata setDPIViaAPI(IIOMetadata imageMetadata, int dpi) throws IIOInvalidTreeException {
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
		TIFFField fieldXRes = new TIFFField(tagXRes, TIFFTag.TIFF_RATIONAL, 1, new long[][] { { dpi, 1 } });
		TIFFField fieldYRes = new TIFFField(tagYRes, TIFFTag.TIFF_RATIONAL, 1, new long[][] { { dpi, 1 } });
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

}
