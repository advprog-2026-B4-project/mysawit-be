package id.ac.ui.cs.advprog.mysawitbe.modules.auth.infrastructure.web;

import id.ac.ui.cs.advprog.mysawitbe.common.dto.ApiResponse;
import id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.dto.UserDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.port.in.AuthCommandUseCase;
import id.ac.ui.cs.advprog.mysawitbe.modules.auth.application.port.in.UserQueryUseCase;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserQueryUseCase   userQueryUseCase;
    private final AuthCommandUseCase authCommandUseCase;

    public UserController(UserQueryUseCase userQueryUseCase,
                          AuthCommandUseCase authCommandUseCase) {
        this.userQueryUseCase   = userQueryUseCase;
        this.authCommandUseCase = authCommandUseCase;
    }

    /** GET /api/users?role=BURUH  (Admin only) */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<UserDTO>>> listUsers(
            @RequestParam(required = false) String role) {
        List<UserDTO> users = userQueryUseCase.listUsers(role);
        return ResponseEntity.ok(ApiResponse.success(users));
    }

    /** GET /api/users/{userId} */
    @GetMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserDTO>> getUser(@PathVariable UUID userId) {
        UserDTO user = userQueryUseCase.getUserById(userId);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    /** PUT /api/users/{userId}  (Admin only) */
    @PutMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<UserDTO>> editUser(
            @PathVariable UUID userId,
            @RequestBody UserDTO body) {
        UserDTO updated = authCommandUseCase.editUser(
                userId, body.name(), body.role(), body.email());
        return ResponseEntity.ok(ApiResponse.success(updated));
    }

    /** DELETE /api/users/{userId}  (Admin only) */
    @DeleteMapping("/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> deleteUser(
            @PathVariable UUID userId,
            @RequestAttribute("userId") UUID requestingAdminId) {
        authCommandUseCase.deleteUser(requestingAdminId, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /** POST /api/users/{buruhId}/assign-mandor/{mandorId}  (Admin only) */
    @PostMapping("/{buruhId}/assign-mandor/{mandorId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<Void>> assignBuruhToMandor(
            @PathVariable UUID buruhId,
            @PathVariable UUID mandorId) {
        authCommandUseCase.assignBuruhToMandor(buruhId, mandorId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    /** GET /api/users/mandor/{mandorId}/buruh — Get all buruh under a mandor */
    @GetMapping("/mandor/{mandorId}/buruh")
    @PreAuthorize("hasAnyRole('ADMIN', 'MANDOR')")
    public ResponseEntity<ApiResponse<List<UserDTO>>> getBuruhByMandor(
            @PathVariable UUID mandorId) {
        List<UserDTO> buruh = userQueryUseCase.getBuruhByMandorId(mandorId);
        return ResponseEntity.ok(ApiResponse.success(buruh));
    }
}