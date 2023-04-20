package com.rubyride.tripmanager.api;

import com.rubyride.api.DataApi;
import com.rubyride.model.DataBlob;
import com.rubyride.model.DataType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
public class DataApiProxy implements DataApi {
  private final DataApiImpl dataApi;

  @Autowired
  public DataApiProxy(final DataApiImpl dataApi) {
    this.dataApi = dataApi;
  }

  @Override
  public ResponseEntity<Void> deleteData(final UUID dataId) {
    return dataApi.deleteData(dataId);
  }

  @Override
  public ResponseEntity<DataBlob> downloadData(final UUID dataId) {
    return dataApi.downloadData(dataId);
  }

  @Override
  public ResponseEntity<List<DataBlob>> getDataByType(final DataType dataType) {
    return dataApi.getDataByType(dataType);
  }

  @Override
  public ResponseEntity<DataBlob> uploadData(final DataBlob data) {
    return dataApi.uploadData(data);
  }
}
