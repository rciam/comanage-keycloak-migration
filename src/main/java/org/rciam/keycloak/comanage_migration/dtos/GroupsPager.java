package org.rciam.keycloak.comanage_migration.dtos;

import org.keycloak.representations.idm.GroupRepresentation;

import java.util.List;

public class GroupsPager {

    private List<GroupRepresentation> results;
    private long count;

    public GroupsPager(List<GroupRepresentation> results, long count){
        this.results = results;
        this.count = count;
    }

    public List<GroupRepresentation> getResults() {
        return results;
    }

    public void setResults(List<GroupRepresentation> results) {
        this.results = results;
    }

    public long getCount() {
        return count;
    }

    public void setCount(long count) {
        this.count = count;
    }
}
