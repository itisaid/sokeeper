package com.sokeeper.client;

import java.util.Collection;
import java.util.Map;

import com.sokeeper.domain.ChangesEvent;
import com.sokeeper.domain.resource.AssociationEntity;
import com.sokeeper.domain.resource.ResourceEntity;
import com.sokeeper.domain.resource.ResourceKey;
import com.sokeeper.exception.RpcException;
import com.sokeeper.handler.ResourceAccessHandler;

public class MockResourceAccessHandler implements ResourceAccessHandler {

    public ResourceEntity addOrUpdateResource(ResourceEntity resourceEntity,
                                              String rightResourceType,
                                              Map<String, AssociationEntity> rightAssociations)
            throws RpcException {
        return null;
    }

    public Long getCurrentSequenceOfChanges() {
        return 1L;
    }

    public ResourceEntity subscribe(ResourceKey resourceKey) throws RpcException {
        return null;
    }

    public ResourceEntity getResourceEntity(ResourceKey resourceKey) throws RpcException {
        return null;
    }

    public void removeResource(ResourceKey resourceKey) throws RpcException {
    }

    public Collection<ChangesEvent> getLostEvents(Long sequenceGot) throws RpcException {
        return null;
    }

    public void subscribe(Collection<ResourceKey> keys) throws RpcException {
    }

    public void unsubscribe(ResourceKey resourceKey) throws RpcException {
    }

    public AssociationEntity addOrUpdateAssociation(ResourceKey leftKey, ResourceKey rightKey,
                                                    Map<String, String> attributes)
            throws RpcException, IllegalStateException {
        return null;
    }

    public AssociationEntity getAssociationEntity(ResourceKey leftKey, ResourceKey rightKey)
            throws RpcException {
        return null;
    }

    public void removeAssociation(ResourceKey leftKey, ResourceKey rightKey) throws RpcException {

    }

}
