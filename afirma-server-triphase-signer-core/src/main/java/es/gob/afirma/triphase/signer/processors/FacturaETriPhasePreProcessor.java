/* Copyright (C) 2011 [Gobierno de Espana]
 * This file is part of "Cliente @Firma".
 * "Cliente @Firma" is free software; you can redistribute it and/or modify it under the terms of:
 *   - the GNU General Public License as published by the Free Software Foundation;
 *     either version 2 of the License, or (at your option) any later version.
 *   - or The European Software License; either version 1.1 or (at your option) any later version.
 * You may contact the copyright holder at: soporte.afirma@seap.minhap.es
 */

package es.gob.afirma.triphase.signer.processors;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.Properties;
import java.util.logging.Logger;

import es.gob.afirma.core.AOException;
import es.gob.afirma.core.AOInvalidSignatureFormatException;
import es.gob.afirma.core.signers.AOSigner;
import es.gob.afirma.core.signers.CounterSignTarget;
import es.gob.afirma.core.signers.TriphaseData;
import es.gob.afirma.signers.xades.AOFacturaESigner;
import es.gob.afirma.signers.xades.EFacturaAlreadySignedException;
import es.gob.afirma.signers.xades.InvalidEFacturaDataException;
import es.gob.afirma.triphase.signer.xades.XAdESTriPhaseSignerServerSide.Op;

/** Procesador de firmas trif&aacute;sicas XAdES FacturaE.
 * @author Tom&aacute;s Garc&iacute;a-Mer&aacute;s. */
public final class FacturaETriPhasePreProcessor extends XAdESTriPhasePreProcessor {

	private static final Logger LOGGER = Logger.getLogger("es.gob.afirma"); //$NON-NLS-1$

	@Override
	public TriphaseData preProcessPreSign(final byte[] data,
			                              final String algorithm,
			                              final X509Certificate[] cert,
			                              final Properties extraParams,
				                          final boolean checkSignatures) throws IOException,
	                                                                           AOException {
		LOGGER.info("Prefirma FacturaE - Firma - INICIO"); //$NON-NLS-1$

		// Con FacturaE solo podemos firmar facturas
		final AOSigner facturaESigner = new AOFacturaESigner();
		if (!facturaESigner.isValidDataFile(data)) {
			throw new InvalidEFacturaDataException();
		}

		// Las facturas solo pueden contener una firma
		if (facturaESigner.isSign(data)) {
			throw new EFacturaAlreadySignedException();
		}

		// Limpiamos los parametros para asegurarnos de que solo se utilizan los parametros XAdES compatibles con FacturaE
		final Properties xParams = AOFacturaESigner.getFacturaEExtraParams(extraParams);

		final TriphaseData presign = preProcessPre(data, algorithm, cert, xParams, Op.SIGN);

		LOGGER.info("Prefirma FacturaE - Firma - FIN"); //$NON-NLS-1$

		return presign;
	}


	@Override
	public byte[] preProcessPostSign(final byte[] data,
			                         final String algorithm,
			                         final X509Certificate[] cert,
			                         final Properties extraParams,
			                         final byte[] session) throws NoSuchAlgorithmException,
			                                                      AOException,
			                                                      IOException {

		return preProcessPostSign(data, algorithm, cert, extraParams, TriphaseData.parser(session));
	}

	@Override
	public byte[] preProcessPostSign(final byte[] data,
			                         final String algorithm,
			                         final X509Certificate[] cert,
			                         final Properties extraParams,
			                         final TriphaseData triphaseData) throws NoSuchAlgorithmException,
			                                                                 AOException,
			                                                                 IOException {

		LOGGER.info("Postfirma FacturaE - Firma - INICIO"); //$NON-NLS-1$

		// Con FacturaE solo podemos firmar facturas
		if (!new AOFacturaESigner().isValidDataFile(data)) {
			throw new AOInvalidSignatureFormatException(
				"Los datos proporcionados no son una factura electronica compatible" //$NON-NLS-1$
			);
		}

		// Limpiamos los parametros para asegurarnos de que solo se utilizan los parametros XAdES compatibles con FacturaE
		final Properties xParams = AOFacturaESigner.getFacturaEExtraParams(extraParams);

		final byte[] postsign = preProcessPost(data, algorithm, cert, xParams, Op.SIGN, triphaseData);

		LOGGER.info("Postfirma FacturaE - Firma - FIN"); //$NON-NLS-1$

		return postsign;
	}


	@Override
	public TriphaseData preProcessPreCoSign(final byte[] data,
			                          final String algorithm,
			                          final X509Certificate[] cert,
			                          final Properties extraParams,
			                          final boolean checkSignatures) throws IOException, AOException {
		throw new UnsupportedOperationException("No se soporta la multifirma de firmas FacturaE"); //$NON-NLS-1$
	}

	@Override
	public TriphaseData preProcessPreCounterSign(final byte[] sign,
			                               final String algorithm,
			                               final X509Certificate[] cert,
			                               final Properties extraParams,
			                               final CounterSignTarget targets,
				                           final boolean checkSignatures) throws IOException,
			                                                                       AOException {
		throw new UnsupportedOperationException("No se soporta la multifirma de firmas FacturaE"); //$NON-NLS-1$
	}


	@Override
	public byte[] preProcessPostCoSign(final byte[] data, final String algorithm, final X509Certificate[] cert, final Properties extraParams,
			final byte[] session) throws NoSuchAlgorithmException, AOException, IOException {
		throw new UnsupportedOperationException("No se soporta la multifirma de firmas FacturaE"); //$NON-NLS-1$
	}

	@Override
	public byte[] preProcessPostCoSign(final byte[] data, final String algorithm, final X509Certificate[] cert, final Properties extraParams,
			final TriphaseData triphaseData) throws NoSuchAlgorithmException, AOException, IOException {
		throw new UnsupportedOperationException("No se soporta la multifirma de firmas FacturaE"); //$NON-NLS-1$
	}

	@Override
	public byte[] preProcessPostCounterSign(final byte[] sign, final String algorithm, final X509Certificate[] cert, final Properties extraParams,
            final byte[] session, final CounterSignTarget targets) throws NoSuchAlgorithmException, AOException, IOException {
		throw new UnsupportedOperationException("No se soporta la multifirma de firmas FacturaE"); //$NON-NLS-1$
	}

	@Override
	public byte[] preProcessPostCounterSign(final byte[] sign, final String algorithm, final X509Certificate[] cert, final Properties extraParams,
            final TriphaseData triphaseData, final CounterSignTarget targets) throws NoSuchAlgorithmException, AOException, IOException {
		throw new UnsupportedOperationException("No se soporta la multifirma de firmas FacturaE"); //$NON-NLS-1$
	}
}
