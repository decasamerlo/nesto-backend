package dev.nesto.port.out;

import dev.nesto.adapter.out.persistence.NodeInMemoryRepository;

class NodeRepositoryPortTest extends NodeRepositoryContractTest {

  @Override
  protected NodeRepositoryPort createRepository() {
    return new NodeInMemoryRepository();
  }
}
