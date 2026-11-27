package dev.nesto.port.in;

import dev.nesto.domain.NodeId;

public interface GetNodeStatusSummaryQuery {

  Result getNodeStatusSummary(NodeId nodeId);

  // Counts of the direct children, an empty status as untracked. No total or percentage: a total
  // is the four summed, and a percentage's denominator is for the surface showing it to choose
  record Result(int done, int inProgress, int open, int untracked) {}
}
