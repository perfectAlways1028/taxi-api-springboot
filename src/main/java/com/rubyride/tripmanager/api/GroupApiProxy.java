package com.rubyride.tripmanager.api;

import com.rubyride.api.GroupApi;
import com.rubyride.model.Group;
import com.rubyride.model.GroupNode;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;
import java.util.List;
import java.util.UUID;

@RestController
public class GroupApiProxy implements GroupApi {
  private final GroupApiImpl groupApi;

  @Autowired
  public GroupApiProxy(final GroupApiImpl groupApi) {
    this.groupApi = groupApi;
  }

  @Override
  public ResponseEntity<Group> addGroup(final Group group) {
    return groupApi.addGroup(group);
  }

  @Override
  public ResponseEntity<Void> deleteGroup(final UUID groupId) {
    return groupApi.deleteGroup(groupId);
  }

  @Override
  public ResponseEntity<Group> getGroupById(final UUID groupId) {
    return groupApi.getGroupById(groupId);
  }

  @Override
  public ResponseEntity<GroupNode> getGroupHierarchy(final UUID groupId, final Integer maxDepth) {
    return groupApi.getGroupHierarchy(groupId, maxDepth);
  }

  @Override
  public ResponseEntity<List<Group>> getGroups() {
    return groupApi.getGroups();
  }

  @Override
  public ResponseEntity<List<Group>> getGroupsForZone(final UUID zoneId) {
    return groupApi.getGroupsForZone(zoneId);
  }

  @Override
  public ResponseEntity<Group> setGroupAgent(final UUID groupId, @Valid final byte[] agent) {
    return groupApi.setGroupAgent(groupId, agent);
  }

  @Override
  public ResponseEntity<Group> setGroupIcon(UUID groupId, @Valid byte[] icon) {
    return groupApi.setGroupIcon(groupId, icon);
  }

  @Override
  public ResponseEntity<Group> setGroupReporting(final UUID groupId, @Valid final byte[] reporting) {
    return groupApi.setGroupReporting(groupId, reporting);
  }

  @Override
  public ResponseEntity<Group> setGroupSalesTemplate(UUID groupId, @Valid byte[] salesTemplate) {
    return groupApi.setGroupSalesTemplate(groupId, salesTemplate);
  }

  @Override
  public ResponseEntity<Group> setGroupServiceLevelAgreement(final UUID groupId, @Valid final byte[] serviceLevelAgreement) {
    return groupApi.setGroupServiceLevelAgreement(groupId, serviceLevelAgreement);
  }

  @Override
  public ResponseEntity<Group> setGroupTerms(final UUID groupId, @Valid final byte[] terms) {
    return groupApi.setGroupTerms(groupId, terms);
  }

  @Override
  public ResponseEntity<Group> updateGroup(final Group group) {
    return groupApi.updateGroup(group);
  }
}
