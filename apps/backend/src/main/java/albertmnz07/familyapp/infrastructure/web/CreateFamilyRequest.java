package albertmnz07.familyapp.infrastructure.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateFamilyRequest(@NotBlank @Size(max = 100) String name) {}
