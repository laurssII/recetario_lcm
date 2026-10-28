package es.uvigo.dagss.recetas.web;

import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;

import es.uvigo.dagss.recetas.dto.LoginRequest;
import es.uvigo.dagss.recetas.dto.LoginResponseDTO;
import es.uvigo.dagss.recetas.services.AuthService;
import jakarta.validation.Valid;

@Tag(name = "Autenticación")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @Operation(summary = "Iniciar sesión",
            description = "Valida usuario y contraseña (el usuario no distingue mayúsculas) y devuelve un token JWT que se envía en el resto de peticiones como `Authorization: Bearer <token>`. Un usuario inexistente, una contraseña incorrecta y una cuenta desactivada devuelven el mismo error.")
    @SecurityRequirements
    @PostMapping("/login")
    public LoginResponseDTO login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }
}
