package platform.common.dto;

import java.util.List;

public record SearchResponse<T>(List<T> data, PaginationResponse pagination) {}
