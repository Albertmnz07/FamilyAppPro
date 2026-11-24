package albertmnz07.familyapp.legacy.family.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateFamilyRequest(@NotBlank @Size(max = 100) String name) {}
