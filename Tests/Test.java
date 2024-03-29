/*
 * NO FUNCIONA
 */
package Pruebas;

import general.Global;
import general.MemoryException;
import general.Global.TiposReemplazo;

import pckMemoria.Cache;
import pckMemoria.CacheAsociativa;
import pckMemoria.MemoriaPrincipal;
import pckMemoria.Pagina;

public class Test {
	
	public Test()
	{
		try
		{
		    int num = 182669919;
			int num2 = 0xFFFFFFFF;
			int num3 = 0b111111111111111;
			int num4 = 32768;
			System.out.println(Integer.toHexString(num));
			System.out.printf("0x%hh \n", num);
			
			System.out.println(Integer.toBinaryString(num2));
			
			System.out.println(num2);
			
			System.out.println(num3);
			
			System.out.println("StringFormat: " + String.format("%16s", Integer.toBinaryString(num3)).replace(" ", "0"));
			
			System.out.println("Bits: " + Global.bitsNecesarios(num));
			System.out.println("Bits dir: " + Global.bitsDireccionar(num4));
			
			// Memoria
			System.out.println("--- Memoria --- \n");
			/*Pagina m1 = new Pagina(32, 4);
			Pagina m2 = new Pagina(16, 4);
			
			for (int i = 0; i < 128; i+=4)
				m1.guardarDato(i, i * 2);
			
			System.out.println(m1.toString());
			
			//System.out.println("Leer 0x40,2: " + Arrays.toString(m1.leerLinea(0x40, 2)));
			
			m2.guardarLinea(0, m1.leerLinea(0x40, 12));
			
			System.out.println(m2.toString());*/
			
			/*Cache c = new CacheDirecta(16, 4);
			for (int i = 0; i < 16*4*4; i+=4)
				c.escribirLinea(i, new int[]{1,2,3,4});
			
			System.out.println(c.toString());
			
			System.out.println(c.consultarDato(0x04));
			
			c.escribirLinea(0x40, m1.leerLinea(0x20, 4));
			
			System.out.println(c.toString());
			
			System.out.println("Dato 0x14: " + c.lineaLibre(0x14) + c.existeDato(0x14) + c.lineaDirty(0x14));
			if (c.existeDato(0x14))
				System.out.println(c.consultarDato(0x14));
			System.out.println("Dato 0x4C: " + c.lineaLibre(0x4C) + c.existeDato(0x4C) + c.lineaDirty(0x4C));
			if (c.existeDato(0x4C))
				System.out.println(c.consultarDato(0x4C));
			System.out.println("Dato 0xE0: " + c.lineaLibre(0xE0) + c.existeDato(0xE0) + c.lineaDirty(0xE0));
			if (c.existeDato(0xE0))
				System.out.println(c.consultarDato(0xE0));*/
			
			System.out.println("Cache Asociativa: \n");
			CacheAsociativa c = new CacheAsociativa(16, 4, 4, TiposReemplazo.LFU);
			
			/*c.escribirLinea(0x0000, new int[]{0,1,2,3});
			c.escribirLinea(0xFF00, new int[]{99,100,101,102});
			c.escribirLinea(0xCC00, new int[]{55,56,57,25});*/
			System.out.println(c.politica.toString());
			//c.modificarDato(0x20, 76);
			
			System.out.println(c.toString());
			
			System.out.println(c.existeDato(0x04));
			System.out.println(c.consultarDato(0x04));
			
			System.out.println(c.existeDato(0xFF04));
			System.out.println(c.consultarDato(0xFF04));
			
			/*c.modificarDato(0xFF04, 900);
			
			System.out.println(c.politica.toString());
			
			System.out.println("Reemplazo: " + c.reemplazarLinea(0x1400, new int[]{5,6,7,8}));
			System.out.println(c.politica.toString());
			System.out.println("Reemplazo: " + c.reemplazarLinea(0x1000, new int[]{55,66,77,88}));*/
			
			System.out.println(c.toString());
			//c.modificarDato(0x14, 0, true);
			
			//System.out.println("Leer: " + Arrays.toString(c.leerLinea(0xFF)));
		}
		catch (MemoryException e)
		{
			System.err.println("ERROR: " + e);
		}
	}
}
