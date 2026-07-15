package pe.edu.utp.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.List;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "El nombre es obligatorio")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$", message = "El nombre solo debe contener letras")
    @Column(nullable = false, length = 100)
    private String nombres;

    @NotBlank(message = "El apellido es obligatorio")
    @Pattern(regexp = "^[a-zA-ZáéíóúÁÉÍÓÚñÑ\\s]+$", message = "El apellido solo debe contener letras")
    @Column(nullable = false, length = 100)
    private String apellidos;

    @NotBlank(message = "El correo es obligatorio")
    @Pattern(regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,6}$", message = "Ingrese un correo válido (ejemplo@correo.com)")
    @Column(nullable = false, length = 150, unique = true)
    private String correo;

    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 6, message = "La contraseña debe tener al menos 6 caracteres")
    @Column(nullable = false)
    private String contrasena;

    @Column(nullable = false, length = 50)
    private String rol;

    @Column(name = "fecha_registro", insertable = false, updatable = false)
    private LocalDateTime fechaRegistro;

    @OneToMany(mappedBy = "usuario", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<FormaPago> formasPago;

    @OneToMany(mappedBy = "comprador", cascade = CascadeType.ALL)
    private List<Pedido> misCompras;

    @OneToMany(mappedBy = "proveedor", cascade = CascadeType.ALL)
    private List<Pedido> ventasRecibidas;

    public Usuario() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNombres() {
        return nombres;
    }

    public void setNombres(String nombres) {
        this.nombres = nombres;
    }

    public String getApellidos() {
        return apellidos;
    }

    public void setApellidos(String apellidos) {
        this.apellidos = apellidos;
    }

    public String getCorreo() {
        return correo;
    }

    public void setCorreo(String correo) {
        this.correo = correo;
    }

    public String getContrasena() {
        return contrasena;
    }

    public void setContrasena(String contrasena) {
        this.contrasena = contrasena;
    }

    public String getRol() {
        return rol;
    }

    public void setRol(String rol) {
        this.rol = rol;
    }

    public LocalDateTime getFechaRegistro() {
        return fechaRegistro;
    }

    public void setFechaRegistro(LocalDateTime fechaRegistro) {
        this.fechaRegistro = fechaRegistro;
    }

    public List<FormaPago> getFormasPago() {
        return formasPago;
    }

    public void setFormasPago(List<FormaPago> formasPago) {
        this.formasPago = formasPago;
    }

    public List<Pedido> getMisCompras() {
        return misCompras;
    }

    public void setMisCompras(List<Pedido> misCompras) {
        this.misCompras = misCompras;
    }

    public List<Pedido> getVentasRecibidas() {
        return ventasRecibidas;
    }

    public void setVentasRecibidas(List<Pedido> ventasRecibidas) {
        this.ventasRecibidas = ventasRecibidas;
    }

}