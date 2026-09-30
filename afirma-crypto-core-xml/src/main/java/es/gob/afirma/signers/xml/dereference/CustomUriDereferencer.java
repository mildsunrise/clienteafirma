/* Copyright (C) 2011 [Gobierno de Espana]
 * This file is part of "Cliente @Firma".
 * "Cliente @Firma" is free software; you can redistribute it and/or modify it under the terms of:
 *   - the GNU General Public License as published by the Free Software Foundation;
 *     either version 2 of the License, or (at your option) any later version.
 *   - or The European Software License; either version 1.1 or (at your option) any later version.
 * You may contact the copyright holder at: soporte.afirma@seap.minhap.es
 */

package es.gob.afirma.signers.xml.dereference;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Locale;
import java.util.logging.Logger;

import javax.xml.crypto.Data;
import javax.xml.crypto.URIDereferencer;
import javax.xml.crypto.URIReference;
import javax.xml.crypto.URIReferenceException;
import javax.xml.crypto.XMLCryptoContext;
import javax.xml.crypto.dom.DOMURIReference;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import es.gob.afirma.core.misc.LoggerUtil;
import es.gob.afirma.core.misc.SecureXmlBuilder;
import es.gob.afirma.core.misc.http.SSLErrorProcessor;
import es.gob.afirma.core.misc.http.UrlHttpManagerFactory;
import es.gob.afirma.core.misc.http.UrlHttpMethod;
import es.gob.afirma.signers.xml.Utils;

/** Dereferenciador a medida de referencias XML DOM. */
public class CustomUriDereferencer implements URIDereferencer {

	private static final String ID = "Id"; //$NON-NLS-1$

	private static final String DEFAULT_SUN_URI_DEREFERENCER_CLASSNAME =           "org.jcp.xml.dsig.internal.dom.DOMURIDereferencer"; //$NON-NLS-1$
	private static final String DEFAULT_APACHE_URI_DEREFERENCER_CLASSNAME = "org.apache.jcp.xml.dsig.internal.dom.DOMURIDereferencer"; //$NON-NLS-1$

	private static final String DEFAULT_SUN_XML_SIGNATURE_INPUT_CLASSNAME = "com.sun.org.apache.xml.internal.security.signature.XMLSignatureInput"; //$NON-NLS-1$
	private static final String DEFAULT_APACHE_XML_SIGNATURE_INPUT_CLASSNAME =               "org.apache.xml.security.signature.XMLSignatureInput"; //$NON-NLS-1$

	private static final String DEFAULT_SUN_OCTET_STREAM_DATA =           "org.jcp.xml.dsig.internal.dom.ApacheOctetStreamData"; //$NON-NLS-1$
	private static final String DEFAULT_APACHE_OCTET_STREAM_DATA = "org.apache.jcp.xml.dsig.internal.dom.ApacheOctetStreamData"; //$NON-NLS-1$

	private static final String DEFAULT_SUN_NODESET_DATA =           "org.jcp.xml.dsig.internal.dom.ApacheNodeSetData"; //$NON-NLS-1$
	private static final String DEFAULT_APACHE_NODESET_DATA = "org.apache.jcp.xml.dsig.internal.dom.ApacheNodeSetData"; //$NON-NLS-1$

	private final URIDereferencer defaultUriDereferencer;

	private static final Logger LOGGER = Logger.getLogger("es.gob.afirma"); //$NON-NLS-1$

	/** Crea un dereferenciador a medida que act&uacute;a solo cuando falla el dereferenciador por defecto. */
	public CustomUriDereferencer() {
		this.defaultUriDereferencer = Utils.getDOMFactory().getURIDereferencer();
	}

	/**
	 * Crea un dereferenciador a medida que act&uacute;a solo cuando falla el dereferenciador por defecto
	 * @param defaultDereferencer Dereferenciador por defecto
	 */
	public CustomUriDereferencer(final URIDereferencer defaultDereferencer) {
		this.defaultUriDereferencer = defaultDereferencer;
	}

	private static Class<?> getNodesetDataClass() throws ClassNotFoundException {
		try {
			return Class.forName(DEFAULT_APACHE_NODESET_DATA);
		}
		catch (final Exception e) {
			return Class.forName(DEFAULT_SUN_NODESET_DATA);
		}
		catch (final Error e) {
			return Class.forName(DEFAULT_SUN_NODESET_DATA);
		}
	}

	private static Class<?> getOctetStreamDataClass() throws ClassNotFoundException {
		try {
			return Class.forName(DEFAULT_APACHE_OCTET_STREAM_DATA);
		}
		catch (final Exception e) {
			return Class.forName(DEFAULT_SUN_OCTET_STREAM_DATA);
		}
		catch (final Error e) {
			return Class.forName(DEFAULT_SUN_OCTET_STREAM_DATA);
		}
	}

	private static Class<?> getXmlSignatureInputClass() throws ClassNotFoundException {
		try {
			return Class.forName(DEFAULT_APACHE_XML_SIGNATURE_INPUT_CLASSNAME);
		}
		catch (final Exception e) {
			return Class.forName(DEFAULT_SUN_XML_SIGNATURE_INPUT_CLASSNAME);
		}
		catch (final Error e) {
			return Class.forName(DEFAULT_SUN_XML_SIGNATURE_INPUT_CLASSNAME);
		}
	}

	/** Obtiene el dereferenciador XML por defecto del JRE.
	 *	Este sera el de Apache o el de Sun.
	 * @return Dereferenciador XML por defecto del JRE.
	 * @throws ClassNotFoundException Si no se encuentra ni el dereferenciador de Sun ni el de Apache. */
	private static Class<?> getDereferencerClass() throws ClassNotFoundException {
		try {
			return Class.forName(DEFAULT_APACHE_URI_DEREFERENCER_CLASSNAME);
		}
		catch (final Exception e) {
			return Class.forName(DEFAULT_SUN_URI_DEREFERENCER_CLASSNAME);
		}
		catch (final Error e) {
			return Class.forName(DEFAULT_SUN_URI_DEREFERENCER_CLASSNAME);
		}
	}

	/** Obtiene el dereferenciador a medida por defecto de Java.
	 * @return Dereferenciador a medida por defecto de Java
	 * @throws NoSuchFieldException Si falla la reflexi&oacute;n por cambios de las clases internas
	 *                              de Java
	 * @throws SecurityException Si no se tienen permisos para la reflexi&oacute;n
	 * @throws ClassNotFoundException Si falla la reflexi&oacute;n por desaparici&oacute;n de las clases internas
	 *                                de Java
	 * @throws IllegalAccessException Si falla la reflexi&oacute;n por fallos de visibilidad */
	public static URIDereferencer getDefaultDereferencer() throws NoSuchFieldException,
	                                                              ClassNotFoundException,
	                                                              IllegalAccessException {
		final Field instanceField = getDereferencerClass().getDeclaredField("INSTANCE"); //$NON-NLS-1$
    	instanceField.setAccessible(true);
    	return (URIDereferencer) instanceField.get(null);
	}

	@Override
	public Data dereference(final URIReference domRef, final XMLCryptoContext context) throws URIReferenceException {
		try {
			return this.defaultUriDereferencer.dereference(domRef, context);
		}
		catch(final Exception e) {

			// Aqui ha fallado el dereferenciador por defecto, probamos a dereferenciar nosotros

			// Si la referencia es http o https salimos, esta clase es para referencias dentro del mismo contexto XML
			final String uri = domRef.getURI();
			String prefixUriLc = uri.substring(0, Math.min(8, uri.length())).toLowerCase(Locale.getDefault());
			if (prefixUriLc.startsWith("http://") || prefixUriLc.startsWith("https://") //$NON-NLS-1$ //$NON-NLS-2$
					|| prefixUriLc.startsWith("ftp://") || prefixUriLc.startsWith("file://")) { //$NON-NLS-1$ //$NON-NLS-2$
				Logger.getLogger("es.gob.afirma").warning( //$NON-NLS-1$
						"Se ha pedido dereferenciar una URI externa. Bloqueamos la operacion: "  //$NON-NLS-1$
								+ LoggerUtil.getTrimStr(uri));
			}

			final Attr uriAttr = (Attr) ((DOMURIReference)domRef).getHere();

			final Document doc = uriAttr.getOwnerDocument();
            final String uriValue = uriAttr.getNodeValue();

            // Derreferenciacion de todo el XML en firmas enveloped
            if ("".equals(uriValue)) { //$NON-NLS-1$
            	try {
					return getStreamData(doc);
				}
            	catch (final IOException e1) {
					throw new URIReferenceException("Error obteniendo los octetos del XML: " + e1, e1); //$NON-NLS-1$
				}
            }

            final Node targetNode = getNodeByInternalUriReference(uriValue, doc);

            if (targetNode == null) {
            	throw new URIReferenceException(e);
            }

            try {
				return getStreamData(targetNode);
			}
            catch (final IOException e1) {
            	throw new URIReferenceException("Error obteniendo los octetos del XML: " + e1, e1); //$NON-NLS-1$
			}
		}
	}

	/** Obtiene un nodo de un XML a partir de su URI de referencia interna.
	 * @param uriValue Referencia interna del nodo
	 * @param doc Documento XML.
	 * @return Nodo a partir de su URI de referencia interna. */
	public static Node getNodeByInternalUriReference(final String uriValue, final Document doc) {
        // Buscamos el nodo en todo el XML
    	String id = uriValue;
    	if (!uriValue.isEmpty() && uriValue.charAt(0) == '#') {
    		id = uriValue.substring(1);
    	}
    	return getElementById(doc, id);
	}

	protected static Data getStreamData(final Node targetNode) throws IOException {
		try {
			final Class<?> xmlSignatureInputClass = getXmlSignatureInputClass();
			final Constructor<?> xmlSignatureInputConstructor = xmlSignatureInputClass.getConstructor(Node.class);
			final Object in = xmlSignatureInputConstructor.newInstance(targetNode);

			final Method isOctetStreamMethod = xmlSignatureInputClass.getMethod("isOctetStream"); //$NON-NLS-1$
			if (((Boolean) isOctetStreamMethod.invoke(in)).booleanValue()) {
				final Class<?> octetStreamDataClass = getOctetStreamDataClass();
				final Constructor<?> octetStreamDataConstructor = octetStreamDataClass.getConstructor(in.getClass());
				return (Data) octetStreamDataConstructor.newInstance(in);
			}
			final Constructor<?> nodeSetDataConstructor = getNodesetDataClass().getConstructor(in.getClass());
			return (Data) nodeSetDataConstructor.newInstance(in);
		}
		catch (final Exception ioe) {
			throw new IOException(ioe);
		}
		catch (final Error ioe) {
			throw new IOException(ioe);
		}
	}

	/** Busca el primer nodo de un documento XML que tenga un atributo con nombre
	 * <i>Id</i> cuyo valor sea el indicado o <code>null</code> si no se encuentra
	 * ninguno.
	 * @param doc Documento XML
	 * @param nodeId Valor del atributo <i>Id</i> del nodo a buscar
	 * @return Primer nodo de un documento XML que tenga un atributo <i>Id</i> con el
	 *         valor indicado o <code>null</code> si no se encuentra ninguno */
	public static Element getElementById(final Document doc, final String nodeId) {
		if (doc == null || nodeId == null) {
			return null;
		}
	    final NodeList nodeList = doc.getElementsByTagName("*"); //$NON-NLS-1$
	    for (int i = 0, len = nodeList.getLength(); i < len; i++) {
	        final Node node = nodeList.item(i);
	        if (node.getNodeType() == Node.ELEMENT_NODE) {
	        	// Buscamos un atributo 'Id'
	        	final NamedNodeMap nnm = node.getAttributes();
	        	for (int j = 0; j < nnm.getLength(); ++j) {
	        	    final Node attr = nnm.item(j);
	        	    if (ID.equalsIgnoreCase(attr.getNodeName()) && nodeId.equals(attr.getNodeValue()) && node instanceof Element) {
	        	    	return (Element) node;
	        	    }
	        	}
	        }
	    }
		return null;
	}

}