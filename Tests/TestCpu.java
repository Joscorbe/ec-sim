/*
 * NO FUNCIONA
 */
package Pruebas;

import java.awt.Dimension;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.UIManager;

import componentes.Tabla;
import componentes.VentanaLimitada;
import componentes.VentanaOculta;

import general.Config.Conf_Type;
import general.Config.Conf_Type_c;
import general.Global.TiposReemplazo;
import general.Config;
import general.Log;
import general.MemoryException;
import pckCpu.CpuMonociclo;
import pckCpu.CpuException;
import pckCpu.Decoder;
import pckCpu.Instruccion;
import pckMemoria.Cache;
import pckMemoria.CacheAsociativa;
import pckMemoria.CacheDirecta;
import pckMemoria.JerarquiaMemoria;
import pckMemoria.MemoriaPrincipal;
import pckMemoria.TablaPaginas;
import pckMemoria.Tlb;

public class TestCpu {
	
	private int bytes_palabra;
	private int palabras_linea;
	
	private int nivelJerarquiasSeparadas;
	
	public JFrame frameMemoria;
	public Tabla tablaMemoria;
	
	public JFrame[] framesCache1;
	public Tabla[] tablasCache1;
	
	public JFrame[] framesCache2;
	public Tabla[] tablasCache2;
	
	private MemoriaPrincipal memoria;
	private JerarquiaMemoria jmem;
	private JerarquiaMemoria jmem2;  // Jerarqu�a de instrucciones.
	private Cache[] caches1;
	private Cache[] caches2;  // Cach� de instrucciones (por si se separan).
	private TablaPaginas tablaPags;
	private Tlb tlb1;
	private Tlb tlb2;
	
	private CpuMonociclo cpu;
	private int direccion_inst = 0;
	
	private int niveles_cache1;
	private int niveles_cache2;
	private int entradas_caches1[];
	private int entradas_caches2[];
	private int vias_caches1[];
	private int vias_caches2[];
	private TiposReemplazo politicas_caches1[];
	private TiposReemplazo politicas_caches2[];
	
	private boolean tp_alojada;
	
	private boolean tlb_datos;
	private boolean tlb_inst;
	
	private String archivo_cpu = "Prueba.txt";
	private String archivo_traza;
	
	// P�ginas y memoria
	int entradas_pagina = 16;
	int max_entrada = 512;	// �ltima entrada permitida
	int max_ent_mem = 128;	// �ltima entrada en memoria (tama�o de memoria)
	int entradas_tlb = 4;
	int vias_tlb = 1;
	
	private int tlb1_entradas;
	private int tlb1_vias;
	private int tlb2_entradas;
	private int tlb2_vias;
	private TiposReemplazo tlb1_politica;
	private TiposReemplazo tlb2_politica;
	
	public TestCpu()
	{
		leerConfig();
		
		
		// Leo el c�digo.
		if (!Decoder.decodificarArchivo(archivo_cpu))
		{
			System.err.println("Error al decodificar el archivo.");
			return;
		}
		
		// Comprobamos que hay instrucciones.
		if (Decoder.getInstrucciones().size() == 0)
		{
			System.err.println("No se han encontrado instrucciones.");
			return;
		}
		
		try
		{
			inicializarMemoria();
			inicializarInterfaz();
			inicializarCpu();
			
			// Guardo un 1 en todas las posiciones entre 0 y 1000.
			for (int i = 0; i <= 1000; i+=4)
				tablaPags.inicializarDatoMemoriaVirtual(i, i);
			
			// Guardo las instrucciones en memoria.
			for (Instruccion inst : Decoder.getInstrucciones())
			{
				if (inst.esDireccionVirtual())
				{
					System.out.println("++ Instruccion " + inst);
					System.out.println("++ Direcci�n V " + (inst.getDireccion()));
					tablaPags.inicializarDatoMemoriaVirtual(inst.getDireccion(), inst.codificarBinario());
				}
				else
				{
					System.out.println("++ Instruccion " + inst);
					System.out.println("++ Direcci�n V " + (direccion_inst + inst.getDireccion()));
					tablaPags.inicializarDatoMemoriaVirtual(direccion_inst + inst.getDireccion(), inst.codificarBinario());
				}
			}
			
			// Asignamos PC a la primera instrucci�n.
			if (Decoder.getPrimeraInstruccion().esDireccionVirtual())
				cpu.setPC(Decoder.getPrimeraInstruccion().getDireccion());
			else
				cpu.setPC(direccion_inst + Decoder.getPrimeraInstruccion().getDireccion());
			
			// Una vez tenemos el c�digo guardado en memoria, comenzamos la ejecuci�n.
			cpu.ejecutarCodigo();
			
			Log.generarEstadistica();
		}
		catch (MemoryException e)
		{
			System.err.println(e);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	// Leer configuraci�n.
	private void leerConfig()
	{
		bytes_palabra = Config.get(Conf_Type.TAMA�O_PALABRA);
		palabras_linea = Config.get(Conf_Type.TAMA�O_LINEA);
		
		nivelJerarquiasSeparadas = Config.get(Conf_Type.NIVEL_JERARQUIAS_SEPARADAS);
		
		entradas_pagina = Config.get(Conf_Type.ENTRADAS_PAGINA);
		max_ent_mem = Config.get(Conf_Type.NUMERO_ENTRADAS_MEMORIA);
		max_entrada = Config.get(Conf_Type.MAXIMA_ENTRADA_MEMORIA);
		
		archivo_cpu = Config.get(Conf_Type_c.ARCHIVO_CODIGO);
		archivo_traza = Config.get(Conf_Type_c.ARCHIVO_TRAZA);
		
		tp_alojada = Config.get(Conf_Type.TABLA_PAGINAS_ALOJADA) == 1 ? true : false;
		
		// Niveles de cach�
		niveles_cache1 = Config.get(Conf_Type.NIVELES_CACHE_DATOS);
		niveles_cache2 =  Config.get(Conf_Type.NIVELES_CACHE_INSTRUCCIONES);
		
		entradas_caches1 = new int[]{Config.get(Conf_Type.CACHE1_DATOS_ENTRADAS),Config.get(Conf_Type.CACHE2_DATOS_ENTRADAS),Config.get(Conf_Type.CACHE3_DATOS_ENTRADAS)};
		vias_caches1 = new int[]{Config.get(Conf_Type.CACHE1_DATOS_VIAS),Config.get(Conf_Type.CACHE2_DATOS_VIAS),Config.get(Conf_Type.CACHE3_DATOS_VIAS)};	
		entradas_caches2 = new int[]{Config.get(Conf_Type.CACHE1_INSTRUCCIONES_ENTRADAS),Config.get(Conf_Type.CACHE2_INSTRUCCIONES_ENTRADAS),Config.get(Conf_Type.CACHE3_INSTRUCCIONES_ENTRADAS)};
		vias_caches2 = new int[]{Config.get(Conf_Type.CACHE1_INSTRUCCIONES_VIAS),Config.get(Conf_Type.CACHE2_INSTRUCCIONES_VIAS),Config.get(Conf_Type.CACHE3_INSTRUCCIONES_VIAS)};


		politicas_caches1 = new TiposReemplazo[niveles_cache1];
		if (niveles_cache1 > 0 && Config.get(Conf_Type_c.CACHE1_DATOS_POLITICA) != null)
			politicas_caches1[0] = TiposReemplazo.valueOf(Config.get(Conf_Type_c.CACHE1_DATOS_POLITICA));
		if (niveles_cache1 > 1 && Config.get(Conf_Type_c.CACHE2_DATOS_POLITICA) != null)
			politicas_caches1[1] = TiposReemplazo.valueOf(Config.get(Conf_Type_c.CACHE2_DATOS_POLITICA));
		if (niveles_cache1 > 2 && Config.get(Conf_Type_c.CACHE3_DATOS_POLITICA) != null)
			politicas_caches1[2] = TiposReemplazo.valueOf(Config.get(Conf_Type_c.CACHE3_DATOS_POLITICA));
		
		if (nivelJerarquiasSeparadas > 1)
		{
			politicas_caches2 = new TiposReemplazo[niveles_cache2];
			if (niveles_cache2 > 0 && Config.get(Conf_Type_c.CACHE1_INSTRUCCIONES_POLITICA) != null)
				politicas_caches2[0] = TiposReemplazo.valueOf(Config.get(Conf_Type_c.CACHE1_INSTRUCCIONES_POLITICA));
			if (niveles_cache2 > 1 && Config.get(Conf_Type_c.CACHE2_INSTRUCCIONES_POLITICA) != null)
				politicas_caches2[1] = TiposReemplazo.valueOf(Config.get(Conf_Type_c.CACHE2_INSTRUCCIONES_POLITICA));
			if (niveles_cache2 > 2 && Config.get(Conf_Type_c.CACHE3_INSTRUCCIONES_POLITICA) != null)
				politicas_caches2[2] = TiposReemplazo.valueOf(Config.get(Conf_Type_c.CACHE3_INSTRUCCIONES_POLITICA));
		}

		tlb_datos = Config.get(Conf_Type.TLB_DATOS) == 1 ? true : false;
		tlb_inst = Config.get(Conf_Type.TLB_INSTRUCCIONES) == 1 ? true : false;
		
		tlb1_entradas = Config.get(Conf_Type.TLB_DATOS_ENTRADAS);
		tlb1_vias = Config.get(Conf_Type.TLB_DATOS_VIAS);
		
		if (tlb_datos)
			tlb1_politica = TiposReemplazo.valueOf(Config.get(Conf_Type_c.TLB_DATOS_POLITICA));
		
		tlb2_entradas = Config.get(Conf_Type.TLB_INSTRUCCIONES_ENTRADAS);
		tlb2_vias = Config.get(Conf_Type.TLB_INSTRUCCIONES_VIAS);
		
		if (tlb_inst)
			tlb2_politica = TiposReemplazo.valueOf(Config.get(Conf_Type_c.TLB_INSTRUCCIONES_POLITICA));
	}
	
	// Inicializa la Cpu.
	private void inicializarCpu() throws CpuException
	{
		// Calculo la direcci�n de memoria para instrucciones.
		int num_instrucciones = Decoder.getInstrucciones().size();
		int paginas_instrucciones = (int) Math.ceil(num_instrucciones / entradas_pagina);
		
		
		if (tablaPags.getNumeroPaginas() == 1)
		{
			int mitad = entradas_pagina / 2;
			if (mitad < num_instrucciones)
				throw new CpuException("No hay memoria suficiente para este c�digo.");
			
			direccion_inst = mitad;
		}
		else
		{
			int primera_pag_inst = tablaPags.getNumeroPaginas()-1 - paginas_instrucciones;
			direccion_inst = primera_pag_inst * tablaPags.getEntradasPagina() * 4;
		}
		
		cpu = new CpuMonociclo(jmem, null, direccion_inst);
	}
	
	// Inicializa la Jerarqu�a de Memoria.
	private void inicializarMemoria() throws MemoryException, CpuException
	{
		caches1 = new Cache[niveles_cache1];
		for (int i = 0; i < niveles_cache1; i++)
		{
			if (vias_caches1[i] > 1)
				caches1[i] = new CacheAsociativa(entradas_caches1[i], palabras_linea, vias_caches1[i], politicas_caches1[i], bytes_palabra);
			else
				caches1[i] = new CacheDirecta(entradas_caches1[i], palabras_linea, bytes_palabra);
		}
		
		// Si hay alguna separaci�n inicializo las de instrucciones.
		if (nivelJerarquiasSeparadas != 1)
		{
			caches2 = new Cache[niveles_cache2];
			
			if (nivelJerarquiasSeparadas < 4)
			{
				if (nivelJerarquiasSeparadas == 2)
				{
					// Si a partir de la 2 son compartidas, creo la primera.
					if (vias_caches2[0] > 1)
						caches2[0] = new CacheAsociativa(entradas_caches2[0], palabras_linea, vias_caches2[0], politicas_caches2[0], bytes_palabra);
					else
						caches2[0] = new CacheDirecta(entradas_caches2[0], palabras_linea, bytes_palabra);
					
					// Las otras 2 son las mismas que las de datos:
					if (niveles_cache2 > 1)
						caches2[1] = caches1[1];
					if (niveles_cache2 > 2)
					caches2[2] = caches2[2];
				}
				else
				{
					// S�lo la 3 es compartida, creo la 1 y la 2
					if (vias_caches2[0] > 1)
						caches2[0] = new CacheAsociativa(entradas_caches2[0], palabras_linea, vias_caches2[0], politicas_caches2[0], bytes_palabra);
					else
						caches2[0] = new CacheDirecta(entradas_caches2[0], palabras_linea, bytes_palabra);
					
					if (niveles_cache2 > 1)
					{
						if (vias_caches2[1] > 1)
							caches2[1] = new CacheAsociativa(entradas_caches2[1], palabras_linea, vias_caches2[1], politicas_caches2[1], bytes_palabra);
						else
							caches2[1] = new CacheDirecta(entradas_caches2[1], palabras_linea, bytes_palabra);
					}
					
					// La �ltima es la misma que la de datos.
					if (niveles_cache2 > 2)
						caches2[2] = caches2[2];
				}
			}
			else
			{
				// Todas separadas.
				for (int i = 0; i < niveles_cache2; i++)
				{
					if (vias_caches2[i] > 1)
						caches2[i] = new CacheAsociativa(entradas_caches2[i], palabras_linea, vias_caches2[i], politicas_caches2[i], bytes_palabra);
					else
						caches2[i] = new CacheDirecta(entradas_caches2[i], palabras_linea, bytes_palabra);
				}
			}
		}
		
		if (tlb_datos)
		{
			if (tlb1_vias > 1)
				tlb1 = new Tlb(tlb1_entradas, tlb1_vias, tlb1_politica);
			else
				tlb1 = new Tlb(tlb1_entradas);
		}
		if (tlb_inst)
		{
			if (tlb2_vias > 1)
				tlb2 = new Tlb(tlb2_entradas, tlb2_vias, tlb2_politica);
			else
				tlb2 = new Tlb(tlb2_entradas);
		}

		// Tabla de P�ginas
		tablaPags = new TablaPaginas(entradas_pagina, palabras_linea, max_entrada, max_ent_mem, TiposReemplazo.LRU, tlb1, tlb2, tp_alojada);
		
		// Memoria principal.
		memoria = new MemoriaPrincipal(tablaPags);
		
		// Inicializar la Jerarqu�a de Memoria.
		jmem = new JerarquiaMemoria(tablaPags, caches1, memoria, false);
		if (nivelJerarquiasSeparadas > 1)
			jmem2 = new JerarquiaMemoria(tablaPags, caches2, memoria, true);
		
		tablaPags.setJerarquiaMemoria(jmem, jmem2);
	}
	
	// Inicializa la interfaz gr�fica.
	private void inicializarInterfaz() throws Exception
	{
		UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		
		tablaMemoria = new Tabla(memoria);
		frameMemoria = new VentanaLimitada();
		JScrollPane jscroll1 = new JScrollPane(tablaMemoria, JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		frameMemoria.setTitle("Memoria");
		frameMemoria.setPreferredSize( new Dimension(245, 400) );
		frameMemoria.setMinimumSize(new Dimension(250, 400));
		frameMemoria.setMaximumSize(new Dimension(400, 2000));
		frameMemoria.add( jscroll1 );
		frameMemoria.pack();
		frameMemoria.addWindowListener(new VentanaOculta(frameMemoria));
		frameMemoria.setVisible(true);
		memoria.setInterfaz(tablaMemoria);
		
		tablasCache1 = new Tabla[niveles_cache1];
		framesCache1 = new JFrame[niveles_cache1];
		
		for (int i = 0; i < niveles_cache1; i++)
		{
			tablasCache1[i] = new Tabla(caches1[i]);
			if (vias_caches1[i] > 1)
				tablasCache1[i].setRenderTablaEnCelda();
			framesCache1[i] = new VentanaLimitada();
			JScrollPane jscroll = new JScrollPane(tablasCache1[i], JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
			framesCache1[i].setTitle("Cache Datos L"+(i));
			framesCache1[i].setPreferredSize( new Dimension(600, 200) );
			framesCache1[i].setMinimumSize(new Dimension(500, 200));
			framesCache1[i].setMaximumSize(new Dimension(2000, 2000));
			framesCache1[i].add( jscroll );
			framesCache1[i].pack();
			framesCache1[i].addWindowListener(new VentanaOculta(framesCache1[i]));
			framesCache1[i].setVisible(true);
			caches1[i].setInterfaz(tablasCache1[i]);
		}
		
		if (nivelJerarquiasSeparadas > 1)
		{
			tablasCache2 = new Tabla[niveles_cache2];
			framesCache2 = new JFrame[niveles_cache2];
			
			for (int i = 0; i < niveles_cache1; i++)
			{
				tablasCache2[i] = new Tabla(caches1[i]);
				if (vias_caches2[i] > 1)
					tablasCache2[i].setRenderTablaEnCelda();
				framesCache2[i] = new VentanaLimitada();
				JScrollPane jscroll = new JScrollPane(tablasCache2[i], JScrollPane.VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
				framesCache2[i].setTitle("Cache Instrucciones L"+(i));
				framesCache2[i].setPreferredSize( new Dimension(600, 200) );
				framesCache2[i].setMinimumSize(new Dimension(500, 200));
				framesCache2[i].setMaximumSize(new Dimension(2000, 2000));
				framesCache2[i].add( jscroll );
				framesCache2[i].pack();
				framesCache2[i].addWindowListener(new VentanaOculta(framesCache2[i]));
				framesCache2[i].setVisible(true);
				caches1[i].setInterfaz(tablasCache2[i]);
			}
		}
	}
}
