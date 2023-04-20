package com.rubyride.tripmanager.api;

import com.rubyride.model.DataType;
import com.rubyride.model.Group;
import com.rubyride.model.GroupNode;
import com.rubyride.tripmanager.exception.CyclicGroupsException;
import com.rubyride.tripmanager.exception.EntityNotFoundException;
import com.rubyride.tripmanager.repository.mongo.GroupRepository;
import com.rubyride.tripmanager.utility.DataRepositoryUtils;
import com.rubyride.tripmanager.utility.StreamUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;

import javax.validation.Valid;
import java.net.URI;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class GroupApiImpl {
  private final GroupRepository groupRepository;
  private final DataRepositoryUtils dataRepositoryUtils;

  public GroupApiImpl(final GroupRepository groupRepository, final DataRepositoryUtils dataRepositoryUtils) {
    this.groupRepository = groupRepository;
    this.dataRepositoryUtils = dataRepositoryUtils;
  }

  @PreAuthorize("@accessControl.canModifyGroups()")
  public ResponseEntity<Group> addGroup(@Valid final Group group) {
    group.setId(UUID.randomUUID());

    groupRepository.insert(group);

    return ResponseEntity.created(URI.create("/v1/groups/" + group.getId().toString()))
        .body(group);
  }

  @PreAuthorize("@accessControl.canModifyGroups()")
  public ResponseEntity<Void> deleteGroup(final UUID groupId) {
    groupRepository.findById(groupId)
        .ifPresent(group -> {
          dataRepositoryUtils.removeAllReferences(groupId);
          groupRepository.findByParentGroupId(groupId)
              .forEach(childGroup ->
                  groupRepository.save(childGroup.parentGroupId(null)));
          groupRepository.deleteById(groupId);
        });

    return ResponseEntity.noContent()
        .build();
  }

  public ResponseEntity<Group> getGroupById(final UUID groupId) {
    return ResponseEntity.of(groupRepository.findById(groupId));
  }

  public ResponseEntity<GroupNode> getGroupHierarchy(final UUID groupId, final Integer maxDepth) {
    return groupRepository.findById(groupId)
        .map(group -> ResponseEntity.of(getGroupHierarchy(group,
            0,
            maxDepth == null ?
                Integer.MAX_VALUE :
                maxDepth,
            new HashSet<>())))
        .orElseThrow(() -> new EntityNotFoundException("Group not found"));
  }

  private Optional<GroupNode> getGroupHierarchy(final Group group, final int currentDepth, final int maxDepth, final Set<Group> currentGroups) {
    if (currentDepth > maxDepth) {
      return Optional.empty();
    }

    if (!currentGroups.add(group)) {
      throw new CyclicGroupsException("Cycle detected in group hierarchy");
    }

    return Optional.of(new GroupNode()
        .id(group.getId())
        .parentGroupId(group.getParentGroupId())
        .ownerId(group.getOwnerId())
        .name(group.getName())
        .eligibilityId(group.getEligibilityId())
        .sponsorshipId(group.getSponsorshipId())
        .fareId(group.getFareId())
        .originZoneId(group.getOriginZoneId())
        .destinationSortId(group.getDestinationSortId())
        .privacyId(group.getPrivacyId())
        .termId(group.getTermId())
        .serviceLevelAgreementId(group.getServiceLevelAgreementId())
        .reportingId(group.getReportingId())
        .agentId(group.getAgentId())
        .children(
            StreamUtils.safeStream(groupRepository.findByParentGroupId(group.getId()))
                .map(childGroup -> getGroupHierarchy(childGroup, currentDepth + 1, maxDepth, currentGroups))
                .flatMap(Optional::stream)
                .collect(Collectors.toList())));
  }

  public ResponseEntity<List<Group>> getGroups() {
    return ResponseEntity.ok(groupRepository.findAll());
  }

  public ResponseEntity<List<Group>> getGroupsForZone(final UUID zoneId) {
    return ResponseEntity.ok(groupRepository.findByOriginZoneId(zoneId));
  }

  @PreAuthorize("@accessControl.canModifyGroups()")
  public ResponseEntity<Group> setGroupAgent(final UUID groupId, @Valid final byte[] agent) {
    return groupRepository.findById(groupId)
        .map(group -> {
          dataRepositoryUtils.removeReference(group.getAgentId(), group.getId());

          final var agentId = agent.length > 0 ?
              dataRepositoryUtils.createData(null, null, DataType.GROUP_AGENT, agent, group.getId()) :
              null;

          return updateGroup(group.agentId(agentId));
        })
        .orElseThrow(() -> new EntityNotFoundException("Group not found"));
  }

  @PreAuthorize("@accessControl.canModifyGroups()")
  public ResponseEntity<Group> setGroupIcon(final UUID groupId, @Valid final byte[] icon) {
    return groupRepository.findById(groupId)
        .map(group -> {
          dataRepositoryUtils.removeReference(group.getIconId(), group.getId());

          final var iconId = icon.length > 0 ?
              dataRepositoryUtils.createData(null, null, DataType.GROUP_ICON, icon, group.getId()) :
              null;

          return updateGroup(group.iconId(iconId));
        })
        .orElseThrow(() -> new EntityNotFoundException("Group not found"));
  }

  @PreAuthorize("@accessControl.canModifyGroups()")
  public ResponseEntity<Group> setGroupReporting(final UUID groupId, @Valid final byte[] reporting) {
    return groupRepository.findById(groupId)
        .map(group -> {
          dataRepositoryUtils.removeReference(group.getReportingId(), group.getId());

          final var reportingId = reporting.length > 0 ?
              dataRepositoryUtils.createData(null, null, DataType.GROUP_REPORTING, reporting, group.getId()) :
              null;

          return updateGroup(group.reportingId(reportingId));
        })
        .orElseThrow(() -> new EntityNotFoundException("Group not found"));
  }

  @PreAuthorize("@accessControl.canModifyGroups()")
  public ResponseEntity<Group> setGroupSalesTemplate(final UUID groupId, @Valid final byte[] salesTemplate) {
    return groupRepository.findById(groupId)
        .map(group -> {
          dataRepositoryUtils.removeReference(group.getSalesTemplateId(), group.getId());

          final var salesTemplateId = salesTemplate.length > 0 ?
              dataRepositoryUtils.createData(null, null, DataType.GROUP_SALES_TEMPLATE, salesTemplate, group.getId()) :
              null;

          return updateGroup(group.salesTemplateId(salesTemplateId));
        })
        .orElseThrow(() -> new EntityNotFoundException("Group not found"));
  }

  @PreAuthorize("@accessControl.canModifyGroups()")
  public ResponseEntity<Group> setGroupServiceLevelAgreement(final UUID groupId, @Valid final byte[] serviceLevelAgreement) {
    return groupRepository.findById(groupId)
        .map(group -> {
          dataRepositoryUtils.removeReference(group.getServiceLevelAgreementId(), group.getId());

          final var serviceLevelAgreementId = serviceLevelAgreement.length > 0 ?
              dataRepositoryUtils.createData(null, null, DataType.GROUP_SERVICE_LEVEL_AGREEMENT, serviceLevelAgreement, group.getId()) :
              null;

          return updateGroup(group.serviceLevelAgreementId(serviceLevelAgreementId));
        })
        .orElseThrow(() -> new EntityNotFoundException("Group not found"));
  }

  @PreAuthorize("@accessControl.canModifyGroups()")
  public ResponseEntity<Group> setGroupTerms(final UUID groupId, @Valid final byte[] terms) {
    return groupRepository.findById(groupId)
        .map(group -> {
          dataRepositoryUtils.removeReference(group.getTermId(), group.getId());

          final var termId = terms.length > 0 ?
              dataRepositoryUtils.createData(null, null, DataType.GROUP_TERMS, terms, group.getId()) :
              null;

          return updateGroup(group.termId(termId));
        })
        .orElseThrow(() -> new EntityNotFoundException("Group not found"));
  }

  @PreAuthorize("@accessControl.canModifyGroups()")
  public ResponseEntity<Group> updateGroup(@Valid final Group group) {
    return groupRepository.findById(group.getId())
        .map(existingGroup -> {
          if (group.getParentGroupId() != null) {
            existingGroup.setParentGroupId(group.getParentGroupId());
          }

          if (group.getOwnerId() != null) {
            existingGroup.setOwnerId(group.getOwnerId());
          }

          if (group.getName() != null) {
            existingGroup.setName(group.getName());
          }

          if (group.getEligibilityId() != null) {
            existingGroup.setEligibilityId(group.getEligibilityId());
          }

          if (group.getSponsorshipId() != null) {
            existingGroup.setSponsorshipId(group.getSponsorshipId());
          }

          if (group.getFareId() != null) {
            existingGroup.setFareId(group.getFareId());
          }

          if (group.getOriginZoneId() != null) {
            existingGroup.setOriginZoneId(group.getOriginZoneId());
          }

          if (group.getDestinationSortId() != null) {
            existingGroup.setDestinationSortId(group.getDestinationSortId());
          }

          if (group.getPrivacyId() != null) {
            existingGroup.setPrivacyId(group.getPrivacyId());
          }

          if (group.getTermId() != null) {
            existingGroup.setTermId(group.getTermId());
          }

          if (group.getServiceLevelAgreementId() != null) {
            existingGroup.setServiceLevelAgreementId(group.getServiceLevelAgreementId());
          }

          if (group.getReportingId() != null) {
            existingGroup.setReportingId(group.getReportingId());
          }

          if (group.getAgentId() != null) {
            existingGroup.setAgentId(group.getAgentId());
          }

          if (group.getSalesTemplateId() != null) {
            existingGroup.setSalesTemplateId(group.getSalesTemplateId());
          }

          if (group.getIconId() != null) {
            existingGroup.setIconId(group.getIconId());
          }

          groupRepository.save(existingGroup);

          return ResponseEntity.ok()
              .location(URI.create("/v1/groups/" + group.getId().toString()))
              .body(existingGroup);
        })
        .orElse(ResponseEntity.noContent()
            .build());
  }
}
