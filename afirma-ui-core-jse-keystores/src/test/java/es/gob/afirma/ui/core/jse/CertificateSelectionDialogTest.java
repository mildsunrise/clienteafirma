package es.gob.afirma.ui.core.jse;

import es.gob.afirma.core.keystores.NameCertificateBean;
import es.gob.afirma.keystores.AOKeyStore;
import es.gob.afirma.keystores.AOKeyStoreDialog;
import es.gob.afirma.keystores.AOKeyStoreManager;
import es.gob.afirma.keystores.AOKeyStoreManagerFactory;
import es.gob.afirma.keystores.callbacks.CachePasswordCallback;
import es.gob.afirma.ui.core.jse.certificateselection.CertificateSelectionDialog;
import org.junit.Ignore;
import org.junit.Test;

import javax.swing.*;
import java.awt.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.junit.Assert.*;

/**
 * Di&aacute;logo de selecci&oacute;n de certificados con est&eacute;tica similar al de
 * Windows 7.
 * @author Carlos Gamuci
 */
public class CertificateSelectionDialogTest {


    private static final String CERT_PATH = "multi_almacen.p12"; //$NON-NLS-1$
    private static final String CERT_PASS = "1111"; //$NON-NLS-1$

	private static final Logger LOGGER = Logger.getLogger("es.gob.afirma"); //$NON-NLS-1$

	/** Prueba de di&aacute;logo de selecci&oacute;n de certificados.
	 * @throws Exception En cualquier error. */
    @SuppressWarnings("static-method")
	@Test
	@Ignore
	public void showCertDialogTest() throws Exception {

		final AOKeyStoreManager ksm = AOKeyStoreManagerFactory.getAOKeyStoreManager(
				AOKeyStore.PKCS12,
				ClassLoader.getSystemResource(CERT_PATH).toString().replace("file:/", ""), //$NON-NLS-1$ //$NON-NLS-2$
				null,
				new CachePasswordCallback(CERT_PASS.toCharArray()),
				null,
				false);

		final AOKeyStoreDialog dialog = new AOKeyStoreDialog(ksm, null, true, true, false);
		String alias;
		try {
			alias = dialog.show();
		}
		catch (final Exception e) {
			LOGGER.log(Level.SEVERE, "Error al cargar un certificado a traves del dialogo de seleccion", e); //$NON-NLS-1$
			e.printStackTrace();
			return;
		}

		LOGGER.info("Certificado con numero de serie:\n" + ksm.getCertificate(alias).getSerialNumber()); //$NON-NLS-1$
	}

}