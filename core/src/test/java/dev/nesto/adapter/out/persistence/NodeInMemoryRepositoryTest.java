package dev.nesto.adapter.out.persistence;

import dev.nesto.port.out.NodeRepositoryContractTest;
import dev.nesto.port.out.NodeRepositoryPort;

class NodeInMemoryRepositoryTest extends NodeRepositoryContractTest {

  @Override
  protected NodeRepositoryPort createRepository() {
    return new NodeInMemoryRepository();
  }
}
