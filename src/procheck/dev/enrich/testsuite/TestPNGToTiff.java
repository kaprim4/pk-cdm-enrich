package procheck.dev.enrich.testsuite;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
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

public class TestPNGToTiff {

	public static void main(String[] args) {
		
	}

	public static byte[] compressTiffTGR(byte[] imageBytes) {
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
			IIOMetadata dataNew = setDPIViaAPI(data);
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
}
