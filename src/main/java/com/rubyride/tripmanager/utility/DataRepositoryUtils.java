package com.rubyride.tripmanager.utility;

import com.rubyride.model.DataBlob;
import com.rubyride.model.DataType;
import com.rubyride.tripmanager.repository.mongo.DataBlobRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.UUID;

@Component
public class DataRepositoryUtils {
  private final DataBlobRepository dataRepository;

  @Autowired
  public DataRepositoryUtils(final DataBlobRepository dataRepository) {
    this.dataRepository = dataRepository;
  }

  public void removeReference(final UUID dataId, final UUID referenceId) {
    if (dataId != null) {
      dataRepository.findById(dataId)
          .ifPresent(data -> {
            final var references = ObjectUtils.getOrDefault(data.getReferences(), Collections.<UUID>emptyList());
            references.remove(referenceId);

            if (references.isEmpty()) {
              dataRepository.deleteById(data.getId());
            } else {
              dataRepository.save(data.references(references));
            }
          });
    }
  }

  public void removeAllReferences(final UUID referenceId) {
    if (referenceId != null) {
      dataRepository.findByReferencesContains(referenceId)
          .forEach(data -> {
            final var references = ObjectUtils.getOrDefault(data.getReferences(), Collections.<UUID>emptyList());
            references.remove(referenceId);

            if (references.isEmpty()) {
              dataRepository.deleteById(data.getId());
            } else {
              dataRepository.save(data.references(references));
            }
          });
    }
  }

  public UUID createData(final String name, final String description, final DataType dataType, final byte[] data, final UUID referenceId) {
    final var dataBlob = new DataBlob()
        .id(UUID.randomUUID())
        .name(name)
        .description(description)
        .type(dataType)
        .data(data)
        .addReferencesItem(referenceId);

    dataRepository.insert(dataBlob);

    return dataBlob.getId();
  }

  public void addReference(final UUID dataId, final UUID referenceId) {
    dataRepository.findById(dataId)
        .ifPresent(data -> {
          final var references = ObjectUtils.getOrDefault(data.getReferences(), Collections.<UUID>emptyList());

          if (!references.contains(referenceId)) {
            references.add(referenceId);
          }

          dataRepository.save(data.references(references));
        });
  }
}
