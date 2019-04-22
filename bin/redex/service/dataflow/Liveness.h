/**
 * Copyright (c) Facebook, Inc. and its affiliates.
 *
 * This source code is licensed under the MIT license found in the
 * LICENSE file in the root directory of this source tree.
 */

#pragma once

#include "ControlFlow.h"
#include "MonotonicFixpointIterator.h"
#include "PatriciaTreeSetAbstractDomain.h"

using LivenessDomain = sparta::PatriciaTreeSetAbstractDomain<uint16_t>;

class LivenessFixpointIterator final
    : public sparta::MonotonicFixpointIterator<
          sparta::BackwardsFixpointIterationAdaptor<cfg::GraphInterface>,
          LivenessDomain> {
 public:
  using NodeId = cfg::Block*;

  LivenessFixpointIterator(const cfg::ControlFlowGraph& cfg)
      : MonotonicFixpointIterator(cfg, cfg.blocks().size()) {}

  void analyze_node(const NodeId& block,
                    LivenessDomain* current_state) const override {
    for (auto it = block->rbegin(); it != block->rend(); ++it) {
      if (it->type == MFLOW_OPCODE) {
        analyze_instruction(it->insn, current_state);
      }
    }
  }

  LivenessDomain analyze_edge(
      const EdgeId&,
      const LivenessDomain& exit_state_at_source) const override {
    return exit_state_at_source;
  }

  void analyze_instruction(const IRInstruction* insn,
                           LivenessDomain* current_state) const {
    if (insn->dests_size()) {
      current_state->remove(insn->dest());
    }
    for (size_t i = 0; i < insn->srcs_size(); ++i) {
      current_state->add(insn->src(i));
    }
  }

  LivenessDomain get_live_in_vars_at(const NodeId& block) const {
    return get_exit_state_at(block);
  }

  LivenessDomain get_live_out_vars_at(const NodeId& block) const {
    return get_entry_state_at(block);
  }
};
