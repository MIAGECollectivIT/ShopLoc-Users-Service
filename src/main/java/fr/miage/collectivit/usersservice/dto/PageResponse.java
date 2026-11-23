package fr.miage.collectivit.usersservice.dto;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.List;
import lombok.Builder;

@Builder
@Schema(description = "Generic paginated response payload")
public record PageResponse<T>(
    @ArraySchema(schema = @Schema(description = "Page elements")) List<T> content,
    @Schema(description = "Current page index (0-based)", example = "0") int page,
    @Schema(description = "Requested page size", example = "20") int size,
    @Schema(description = "Total number of elements matching the criteria", example = "42")
        long totalElements,
    @Schema(description = "Total number of pages", example = "3") int totalPages,
    @Schema(description = "Whether this is the first page", example = "true") boolean first,
    @Schema(description = "Whether this is the last page", example = "false") boolean last) {

  public static <T> PageResponse<T> of(
      List<T> content, int page, int size, long totalElements, int totalPages) {
    return PageResponse.<T>builder()
        .content(content)
        .page(page)
        .size(size)
        .totalElements(totalElements)
        .totalPages(totalPages)
        .first(page == 0)
        .last(page >= totalPages - 1 || totalPages == 0)
        .build();
  }
}
