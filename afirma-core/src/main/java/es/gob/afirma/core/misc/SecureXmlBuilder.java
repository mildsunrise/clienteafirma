package es.gob.afirma.core.misc;

import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.xml.sax.EntityResolver;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXNotRecognizedException;
import org.xml.sax.SAXNotSupportedException;

/**
 * Constructor de objetos para la carga de docuemntos XML.
 */
public class SecureXmlBuilder {

	private static DocumentBuilderFactory SECURE_BUILDER_FACTORY = null;

    private static SAXParserFactory SAX_FACTORY = null;

	private static final Logger LOGGER = Logger.getLogger("es.gob.afirma"); //$NON-NLS-1$

	/**
	 * Obtiene un generador de &aacute;boles DOM con el que crear o cargar un XML.
	 * @return Generador de &aacute;rboles DOM.
	 * @throws ParserConfigurationException Cuando ocurre un error durante la creaci&oacute;n.
	 */
	public static DocumentBuilder getSecureDocumentBuilder() throws ParserConfigurationException {
		if (SECURE_BUILDER_FACTORY == null) {
			SECURE_BUILDER_FACTORY = createSecureDocumentBuilderFactory();
		}

		// Existe la posibilidad de establecer un EntityResolver que devuelva un InputSource vacio para cualquier
		// entidad externa, pero esto permitiría cargar XML que no queremos cargar por considerarlo inseguro,
		// por lo que no vamos a hacerlo y dejaremos que falle la carga de aquellos XML que consideramos inseguros.

		return SECURE_BUILDER_FACTORY.newDocumentBuilder();
	}

	private static DocumentBuilderFactory createSecureDocumentBuilderFactory() throws ParserConfigurationException {
		final DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		dbf.setFeature(SecureXmlConstants.FEATURE_SECURE_PROCESSING, true);
		dbf.setFeature(SecureXmlConstants.FEATURE_DISALLOW_DOCTYPE_DECL, true);
		dbf.setFeature(SecureXmlConstants.FEATURE_EXTERNAL_GENERAL_ENTITIES, false);
		dbf.setFeature(SecureXmlConstants.FEATURE_EXTERNAL_PARAMETER_ENTITIES, false);
		dbf.setFeature(SecureXmlConstants.FEATURE_LOAD_EXTERNAL_DTD, false);

		// Los siguientes atributos deberia establececerlos automaticamente la implementacion de
		// la biblioteca al habilitar la caracteristica anterior. Por si acaso, los establecemos
		// expresamente
		final String[] securityProperties = new String[] {
				SecureXmlConstants.ATTRIBUTE_ACCESS_EXTERNAL_DTD,
				SecureXmlConstants.ATTRIBUTE_ACCESS_EXTERNAL_SCHEMA,
				SecureXmlConstants.ATTRIBUTE_ACCESS_EXTERNAL_STYLESHEET
		};
		for (final String securityProperty : securityProperties) {
			try {
				dbf.setAttribute(securityProperty, ""); //$NON-NLS-1$
			}
			catch (final Exception e) {
				// Podemos las trazas en debug, ya que estas propiedades son adicionales
				// a la activacion del procesado seguro y algunas de ellas no estan soportadas por todas
				// las implementaciones de la API
				LOGGER.log(Level.FINE, "No se ha podido establecer una propiedad de seguridad '" + securityProperty + "' en la factoria XML"); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
			}
		}

		dbf.setValidating(false);
		dbf.setNamespaceAware(true);
		dbf.setXIncludeAware(false);
		dbf.setExpandEntityReferences(false);
		return dbf;
	}

	/**
     * Construye un parser SAX seguro que no accede a recursos externos.
     * @return Factor&iacute;a segura.
	 * @throws SAXException Cuando ocurre un error de SAX.
	 * @throws ParserConfigurationException Cuando no se puede crear el parser.
     */
	public static SAXParser getSecureSAXParser() throws ParserConfigurationException, SAXException {
		if (SAX_FACTORY == null) {
			SAX_FACTORY = createSecureSAXParserFactory();
		}
		final SAXParser parser = SAX_FACTORY.newSAXParser();
		final String[] securityProperties = new String[] {
				SecureXmlConstants.ATTRIBUTE_ACCESS_EXTERNAL_DTD,
				SecureXmlConstants.ATTRIBUTE_ACCESS_EXTERNAL_SCHEMA,
				SecureXmlConstants.ATTRIBUTE_ACCESS_EXTERNAL_STYLESHEET
		};
		for (final String securityProperty : securityProperties) {
			setPropertyIfSupported(parser, securityProperty, ""); //$NON-NLS-1$
		}

		// El resolvedor actua como salvaguarda adicional en implementaciones que no
		// soporten alguna de las caracteristicas SAX configuradas en la factoria.
		parser.getXMLReader().setEntityResolver(new EntityResolver() {
			@Override
			public InputSource resolveEntity(final String publicId, final String systemId) {
				return new InputSource(new java.io.StringReader("")); //$NON-NLS-1$
			}
		});
		return parser;
	}

	private static SAXParserFactory createSecureSAXParserFactory() throws SAXNotSupportedException,
			SAXNotRecognizedException, ParserConfigurationException {
		final SAXParserFactory spf = SAXParserFactory.newInstance();
		spf.setFeature(SecureXmlConstants.FEATURE_SECURE_PROCESSING, true);

		// Desactivamos las caracteristicas que permiten la carga de elementos externos.
		// No todas las implementaciones JAXP reconocen todas las caracteristicas SAX.
		spf.setFeature(SecureXmlConstants.FEATURE_EXTERNAL_GENERAL_ENTITIES, false); //$NON-NLS-1$
		spf.setFeature(SecureXmlConstants.FEATURE_EXTERNAL_PARAMETER_ENTITIES, false); //$NON-NLS-1$
		spf.setFeature(SecureXmlConstants.FEATURE_LOAD_EXTERNAL_DTD, false); //$NON-NLS-1$

		spf.setValidating(false);
		spf.setNamespaceAware(true);

		return spf;
	}

//	private static void setFeatureIfSupported(final SAXParserFactory spf, final String feature, final boolean value) {
//		try {
//			spf.setFeature(feature, value);
//		}
//		catch (final SAXNotRecognizedException e) {
//			LOGGER.log(Level.FINE, "La factoria SAX no reconoce la caracteristica '" + feature + "'"); //$NON-NLS-1$ //$NON-NLS-2$
//		}
//		catch (final SAXNotSupportedException e) {
//			LOGGER.log(Level.FINE, "La factoria SAX no soporta la caracteristica '" + feature + "'"); //$NON-NLS-1$ //$NON-NLS-2$
//		}
//		catch (final ParserConfigurationException e) {
//			LOGGER.log(Level.FINE, "No se ha podido configurar la caracteristica '" + feature + "'"); //$NON-NLS-1$ //$NON-NLS-2$
//		}
//	}

	private static void setPropertyIfSupported(final SAXParser parser, final String property, final String value) {
		try {
			parser.setProperty(property, value);
		}
		catch (final SAXNotRecognizedException e) {
			LOGGER.log(Level.FINE, "El parser SAX no reconoce la propiedad '" + property + "'"); //$NON-NLS-1$ //$NON-NLS-2$
		}
		catch (final SAXNotSupportedException e) {
			LOGGER.log(Level.FINE, "El parser SAX no soporta la propiedad '" + property + "'"); //$NON-NLS-1$ //$NON-NLS-2$
		}
	}

}