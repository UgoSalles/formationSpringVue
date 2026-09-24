package platform.common.dto;

public record SortRequest(String sortBy, SortOrder sortOrder) {

    public SortRequest {
        if (sortOrder == null) sortOrder = SortOrder.ASC;
    }
}
