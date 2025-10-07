package procheck.dev.enrich.threads.gab;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import javax.imageio.ImageIO;

import procheck.dev.enrich.CommonVars;

/**
 * Objet qui contient les donn�es valeur pour le traitement GAB
 * 
 * @author K.ABIDA
 *
 */
class ChequeData {
	/**
	 * CMC7
	 */
	String cmc7;

	/**
	 * le nom des fichiers images dans le ZIP GAB
	 */
	String imgName;

	/**
	 * l'ordre de la valeur dans le scan
	 */
	String ordreCheque;

	/**
	 * l'image recto NB de la valeur
	 */
	BufferedImage imgBWFront;

	/**
	 * l'image verso NB de la valeur
	 */
	BufferedImage imgBWRear;

	/**
	 * l'image recto TG de la valeur
	 */
	BufferedImage imgColorFront;

	/**
	 * l'image verso TG de la valeur
	 */
	BufferedImage imgColorRear;

	public ChequeData() {

	}

	/**
	 * Constructeur de l'objet
	 * 
	 * @param cmc7        la valeur de cmc7
	 * @param imgName     le nom des fichiers images dans le ZIP GAB
	 * @param ordreCheque l'ordre de la valeur dans le scan
	 */
	public ChequeData(String cmc7, String imgName, String ordreCheque) {
		super();
		this.cmc7 = cmc7;
		this.imgName = imgName;
		this.ordreCheque = ordreCheque;
	}

	/**
	 * charger les images de la valeur
	 * @param listFileImage la liste des images dans le fichier ZIP
	 * @param zipFile le fichier ZIP GAB
	 * @return true si ok false sinon
	 */
	public boolean putImagesCheque(List<ZipEntry> listFileImage, ZipFile zipFile) {
		boolean isOk = true;
		try {
			CommonVars.logger.info("Start ChequeData.putImagesCheque");
			int idx = 0;
			for (ZipEntry ze : listFileImage) {
				try (InputStream stream = zipFile.getInputStream(ze)) {
	                if (ze.getName().equalsIgnoreCase(this.imgName + ".G")) {
	                    this.imgBWFront = ImageIO.read(stream);
	                    idx++;
	                } else if (ze.getName().equalsIgnoreCase(this.imgName + ".B")) {
	                    this.imgBWRear = ImageIO.read(stream);
	                    idx++;
	                } else if (ze.getName().equalsIgnoreCase(this.imgName + ".R")) {
	                    this.imgColorFront = ImageIO.read(stream);
	                    idx++;
	                } else if (ze.getName().equalsIgnoreCase(this.imgName + ".V")) {
	                    this.imgColorRear = ImageIO.read(stream);
	                    idx++;
	                }
	            } catch (Exception e) {
	                CommonVars.logger.error("Error reading image from stream for " + ze.getName(), e);
	                isOk = false;
	                break;
	            }
				if (idx == 4) {
					break;
				}
			}
			if (idx != 4) {
				CommonVars.logger.error(
						"PCException #ChequeData.putImagesCheque# Nbr Images Incorrect for [" + this.imgName + "]");
				isOk = false;
			}
		} catch (Exception e) {
			CommonVars.logger.error("PCException #ChequeData.putImagesCheque#", e);
			isOk = false;
		}
		CommonVars.logger.info("End ChequeData.putImagesCheque[" + isOk + "]");
		return isOk;
	}

	/**
	 * rectification de la CMC7 envoy� par le scanner GAB
	 * @return cmc7 rectifi�
	 */
	public String getCmc7() {
		String newCMC7 = cmc7;
		newCMC7 = newCMC7.replace("A", " ");
		newCMC7 = newCMC7.replace("E", " ");
		newCMC7 = newCMC7.replace("C", " ");
		return newCMC7;
	}
}