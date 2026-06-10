package pe.edu.utp.model;

public class ItemCarrito {

    private Celular celular;
    private int cantidad;

    public ItemCarrito() {
    }

    public ItemCarrito(Celular celular, int cantidad) {
        this.celular = celular;
        this.cantidad = cantidad;
    }

    public Celular getCelular() {
        return celular;
    }

    public void setCelular(Celular celular) {
        this.celular = celular;
    }

    public int getCantidad() {
        return cantidad;
    }

    public void setCantidad(int cantidad) {
        this.cantidad = cantidad;
    }

    public Double getSubtotal() {
        return this.celular.getPrecio() * this.cantidad;
    }
}