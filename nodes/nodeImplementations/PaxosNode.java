/*
 Copyright (c) 2007, Distributed Computing Group (DCG)
                    ETH Zurich
                    Switzerland
                    dcg.ethz.ch

 All rights reserved.

 Redistribution and use in source and binary forms, with or without
 modification, are permitted provided that the following conditions
 are met:

 - Redistributions of source code must retain the above copyright
   notice, this list of conditions and the following disclaimer.

 - Redistributions in binary form must reproduce the above copyright
   notice, this list of conditions and the following disclaimer in the
   documentation and/or other materials provided with the
   distribution.

 - Neither the name 'Sinalgo' nor the names of its contributors may be
   used to endorse or promote products derived from this software
   without specific prior written permission.

 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
 LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
 A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
 THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
package projects.paxos.nodes.nodeImplementations;


import java.awt.Color;
import java.awt.Graphics;
import java.util.Set;
import java.util.HashSet;

import sinalgo.configuration.WrongConfigurationException;
import sinalgo.gui.transformation.PositionTransformation;
import sinalgo.nodes.Node;
import sinalgo.nodes.messages.Inbox;
import sinalgo.nodes.messages.Message;
import projects.paxos.nodes.messages.AcceptMessage;
import projects.paxos.nodes.messages.AcceptAckMessage;
import projects.paxos.nodes.messages.LearnMessage;
import projects.paxos.nodes.messages.PrepareMessage;
import projects.paxos.nodes.messages.PrepareAckMessage;
import projects.paxos.nodes.messages.TimestampedMessage;

/**
 * The absolute dummy node. Does not do anything. Good for testing network topologies.
 */
public class PaxosNode extends Node {
	// proposer variables
	boolean distinguished = false;
	int currentProposalNumber = 0;
	String currentProposalValue;
	Set<Integer> proposalsAccepted = new HashSet<Integer>();
	Set<Integer> accepts = new HashSet<Integer>();
	int N_NODES = 64;

	// acceptor variables
	int highestAcceptedProposalNumber = 0;
	String acceptedProposalValue;
	boolean prepared = false;
	boolean accepted = false;

	// learner variables
	boolean learned = false;
	String learnedValue = null;

	// logic clock
	int timestamp = 0;

	@Override
	public void handleMessages(Inbox inbox) {
		while(inbox.hasNext()) {
			Message msg = inbox.next();
			Node sender = inbox.getSender();
			// Acceptor
			if (msg instanceof PrepareMessage) {
				PrepareMessage pmsg = (PrepareMessage) msg;
				if (pmsg.number > highestAcceptedProposalNumber) {
					highestAcceptedProposalNumber = pmsg.number;
					acceptedProposalValue = pmsg.value;
					PrepareAckMessage ack = new PrepareAckMessage(
							highestAcceptedProposalNumber, 
							acceptedProposalValue);
					sendTS(ack, sender);
					prepared = true;
				}
			}
			if (msg instanceof AcceptMessage) {
				AcceptMessage amsg = (AcceptMessage) msg;
				if (amsg.number >= highestAcceptedProposalNumber) {
					highestAcceptedProposalNumber = amsg.number;
					acceptedProposalValue = amsg.value;
					AcceptAckMessage ack = new AcceptAckMessage(
							highestAcceptedProposalNumber, 
							acceptedProposalValue);
					sendTS(ack, sender);
					accepted = true;
				}
			}
			// Proposer
			if (msg instanceof PrepareAckMessage) {
				PrepareAckMessage amsg = (PrepareAckMessage) msg;
				if (amsg.number >= currentProposalNumber) {
					currentProposalNumber = amsg.number;
					currentProposalValue = amsg.value;
					proposalsAccepted.add(sender.ID);
				}
			}
			// Learner
			if (msg instanceof AcceptAckMessage) {
				AcceptAckMessage amsg = (AcceptAckMessage) msg;
				if (amsg.number >= currentProposalNumber) {
					currentProposalNumber = amsg.number;
					currentProposalValue = amsg.value;
					accepts.add(sender.ID);
					proposalsAccepted.add(sender.ID);
					if (accepts.size() > N_NODES/2) {
						learned = true;
						learnedValue = currentProposalValue;
					};
				}
			}
			if (msg instanceof LearnMessage) {
				learned = true;
				LearnMessage lmsg = (LearnMessage) msg;
				learnedValue = lmsg.value;
			}
		}
	}

	@Override
	public void preStep() {
		// Proposer
		if (distinguished) {
			if (currentProposalValue == null) {
				currentProposalNumber = 1;
				currentProposalValue = "A";
			}			
		}
	}

	@Override
	public void init() {
		if (this.ID == 1) {
			distinguished = true;
		}
	}

	@Override
	public void neighborhoodChange() {
		// Proposer
		if (distinguished) {
			PrepareMessage pmsg = new PrepareMessage(currentProposalNumber, currentProposalValue);
			broadcastTS(pmsg);
			if (has_majority()) {
				AcceptMessage amsg = new AcceptMessage(currentProposalNumber, currentProposalValue);
				broadcastTS(amsg);
			}
			if (learned) {
				LearnMessage lmsg = new LearnMessage(learnedValue);
				broadcastTS(lmsg);
			}
		}
	}

	private void sendTS(TimestampedMessage ts, Node n) {
		ts.timestamp = timestamp++;
		send((Message) ts, n);
	}

	private void broadcastTS(TimestampedMessage ts) {
		ts.timestamp = timestamp++;
		broadcast((Message) ts);
	}

	@Override
	public void postStep() {}

	@Override
	public String toString() {
		String s = "Node(" + this.ID + ")\n";
		s += "Accepts: " + accepts + "\n";
		s += "Proposal Ack:" + proposalsAccepted;
		return s;
	}

	private boolean has_majority() {
		return proposalsAccepted.size() > N_NODES/2;
	}

	@Override
	public void draw(Graphics g, PositionTransformation pt, boolean highlight) {
		if (learned) {
			setColor(Color.ORANGE);
		} else if (has_majority()) {
			setColor(Color.GREEN);
		} else if (distinguished) {
			setColor(Color.RED);
		} else if (accepted) {
			setColor(Color.CYAN);
		} else if (prepared) {
			setColor(Color.BLUE);
		}
		
		String text = String.valueOf(this.ID);
		if (distinguished) {
			text += " (" + accepts.size() + "/" + proposalsAccepted.size() + ")";
		}
		super.drawNodeAsSquareWithText(g, pt, highlight, text, 25, Color.WHITE);
	}
	
	@Override
	public void checkRequirements() throws WrongConfigurationException {}

}
