package es.gob.afirma.signers.pades;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.lowagie.text.Document;
import com.lowagie.text.Paragraph;
import com.lowagie.text.pdf.PdfArray;
import com.lowagie.text.pdf.PdfDictionary;
import com.lowagie.text.pdf.PdfName;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.PdfWriter;

import org.junit.Assert;
import org.junit.Test;

public class TestPdfUtils {

	/**
	 * Comprueba el funcionamiento del algoritmo de extracci&oacute;n
	 * de los rangos de p&aacute;gina.
	 */
	@Test
	public void testPageRanges() {

		final int TOTAL_PAGES = 10;

		final List<Integer> pages = new ArrayList<>();
		PdfUtil.getPagesRange("7", TOTAL_PAGES, pages);
		checkExpected(pages, new int[] {7});

		pages.clear();
		PdfUtil.getPagesRange(" 3 ", TOTAL_PAGES, pages);
		checkExpected(pages, new int[] {3});

		pages.clear();
		PdfUtil.getPagesRange("5-8", TOTAL_PAGES, pages);
		checkExpected(pages, new int[] {5, 6, 7, 8});

		pages.clear();
		PdfUtil.getPagesRange("8--1", TOTAL_PAGES, pages);
		checkExpected(pages, new int[] {8, 9, 10});

		pages.clear();
		PdfUtil.getPagesRange("-3--1", TOTAL_PAGES, pages);
		checkExpected(pages, new int[] {8, 9, 10});

		pages.clear();
		PdfUtil.getPagesRange(" -3 - -1 ", TOTAL_PAGES, pages);
		checkExpected(pages, new int[] {8, 9, 10});

		pages.clear();
		PdfUtil.getPagesRange("0", TOTAL_PAGES, pages);
		checkExpected(pages, new int[] {1});

		pages.clear();
		PdfUtil.getPagesRange("20", TOTAL_PAGES, pages);
		checkExpected(pages, new int[] {TOTAL_PAGES});

		pages.clear();
		PdfUtil.getPagesRange("-20", TOTAL_PAGES, pages);
		checkExpected(pages, new int[] {1});

		pages.clear();
		try {
			PdfUtil.getPagesRange("5-3", TOTAL_PAGES, pages);
			Assert.fail("Se ha aceptado un rango no valido: 5-3");
		}
		catch (final Exception e) {
			// OK
		}

		pages.clear();
		try {
			PdfUtil.getPagesRange("-1--3", TOTAL_PAGES, pages);
			Assert.fail("Se ha aceptado un rango no valido: -1--3"); //$NON-NLS-1$
		}
		catch (final Exception e) {
			// OK
		}

		pages.clear();
		try {
			PdfUtil.getPagesRange("1a-5", TOTAL_PAGES, pages);
			Assert.fail("Se ha aceptado un rango no valido: 1a-5"); //$NON-NLS-1$
		}
		catch (final Exception e) {
			// OK
		}
	}

	@Test
	public void testPdfAIdentification() {
		Assert.assertFalse(PdfUtil.isPdfA(null));

		final byte[] pdfA1Metadata = "<pdfaid:part>1</pdfaid:part>".getBytes(StandardCharsets.UTF_8); //$NON-NLS-1$
		final byte[] pdfA2Metadata = "<pdfaid:part>2</pdfaid:part>".getBytes(StandardCharsets.UTF_8); //$NON-NLS-1$

		Assert.assertTrue(PdfUtil.isPdfA(pdfA1Metadata));
		Assert.assertTrue(PdfUtil.isPdfA(pdfA2Metadata));
	}

	@Test
	public void testPdfAColorSpacesConfigureCalibratedDefaults() throws Exception {
		final ByteArrayOutputStream inputData = new ByteArrayOutputStream();
		final Document inputDocument = new Document();
		PdfWriter.getInstance(inputDocument, inputData);
		inputDocument.open();
		inputDocument.add(new Paragraph("Test")); //$NON-NLS-1$
		inputDocument.close();

		final PdfReader reader = new PdfReader(inputData.toByteArray());
		final ByteArrayOutputStream outputData = new ByteArrayOutputStream();
		final Document outputDocument = new Document();
		final PdfWriter writer = PdfWriter.getInstance(outputDocument, outputData);
		try {
			outputDocument.open();
			PdfAColorSpaces.configure(writer, reader);
			outputDocument.add(new Paragraph("Test")); //$NON-NLS-1$

			final PdfDictionary defaultColorspaces = writer.getDefaultColorspace();
			final PdfArray defaultGray = defaultColorspaces.getAsArray(PdfName.DEFAULTGRAY);
			final PdfArray defaultRgb = defaultColorspaces.getAsArray(PdfName.DEFAULTRGB);

			Assert.assertNotNull(defaultGray);
			Assert.assertEquals(PdfName.CALGRAY, defaultGray.getAsName(0));
			Assert.assertNotNull(defaultRgb);
			Assert.assertEquals(PdfName.CALRGB, defaultRgb.getAsName(0));
		}
		finally {
			outputDocument.close();
			reader.close();
		}
	}

	private static void checkExpected(final List<Integer> pagesList, final int[] expected) {

		final Integer[] pages = pagesList.toArray(new Integer[0]);

		Assert.assertEquals("No se han cargado todas las paginas del rango", expected.length, pages.length); //$NON-NLS-1$

		Arrays.sort(pages);

		for (int i = 0; i < pages.length; i++) {
			Assert.assertEquals("Encontrada pagina fuera del rango esperado", expected[i], pages[i].intValue()); //$NON-NLS-1$
		}
	}
}