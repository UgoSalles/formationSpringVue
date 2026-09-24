package platform.common.dto;

import java.util.List;
import java.util.Map;

public record SearchRequest(
        PaginationRequest pagination,
        List<SortRequest> sort,
        Map<String, FilterCondition> filters
) {
    public SearchRequest {
        if (pagination == null) pagination = PaginationRequest.defaults();
        if (sort == null) sort = List.of();
        if (filters == null) filters = Map.of();
    }
}
