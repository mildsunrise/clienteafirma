package es.gob.afirma.standalone.ui;

import java.io.File;

/**
 * Clase que atiende peticiones de carga de ficheros.
 */
public interface LoadDataFileListener {

	/**
     * Solicita la carga de unos ficheros.
     *
     * @param files             Listado de ficheros a cargar.
     * @param generalSignConfig Configuraci&oacute;n de firma que se debe aplicar de forma general.
     * @param forceConfig Indica si se debe forzar la aplicaci&oacute;n de la configuraci&oacute;n de firma general
	 *                    a todos los ficheros. Se puede establecer a <code>false</code> para que se utilice unicamente
	 *                    como configuraci&oacute;n general para las firmas de lote.
     */
	void loadFiles(File[] files, SignOperationConfig generalSignConfig);
}