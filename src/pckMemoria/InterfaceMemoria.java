package pckMemoria;

public interface Cache {
	
	public boolean existeDato(int direccion);
	public boolean isDirty(int direccion);
	public boolean isValid(int direccion);
	
	public int getTamanoLinea();
	
	public int leerDato(int direccion);
	public void guardarDato(int direccion, int dato);
	public int[] leerLinea(int direccion, int tam_linea);
	public void guardarLinea(int direccion, int[] linea);
	
	public String toString(boolean mostrarTodos);

}
