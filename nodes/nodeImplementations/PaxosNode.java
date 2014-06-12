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
import java.util.Iterator;

import sinalgo.configuration.WrongConfigurationException;
import sinalgo.gui.transformation.PositionTransformation;
import sinalgo.nodes.Node;
import sinalgo.nodes.edges.Edge;
import sinalgo.nodes.messages.Inbox;
import sinalgo.nodes.messages.Message;
import projects.paxos.nodes.messages.PrepareMessage;
import projects.paxos.nodes.messages.AcceptMessage;

/**
 * The absolute dummy node. Does not do anything. Good for testing network topologies.
 */
public class PaxosNode extends Node {
	// proposer variables
	boolean distinguished = false;
	int currentProposalNumber = 0;
	String currentProposalValue;
	int proposalsAccepted = 0;
	
	// acceptor variables
	int highestAcceptedProposalNumber = 0;
	String acceptedProposalValue;

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
					AcceptMessage amsg = new AcceptMessage(
							highestAcceptedProposalNumber, 
							acceptedProposalValue);
					send(amsg, sender);
					setColor(Color.GREEN);
				}
			}
			// Proposer
			if (msg instanceof AcceptMessage) {
				AcceptMessage amsg = (AcceptMessage) msg;
				if (amsg.number >= currentProposalNumber) {
					currentProposalNumber = amsg.number;
					currentProposalValue = amsg.value;
					proposalsAccepted++;
				}
			}
		}
	}

	@Override
	public void preStep() {
		if (currentProposalValue == null) {
			currentProposalNumber = 1;
			currentProposalValue = "A";
		}
	}

	@Override
	public void init() {
		if (this.ID == 1) {
			distinguished = true;
			setColor(Color.RED);
		}
	}

	@Override
	public void neighborhoodChange() {
		if (distinguished) {
			PrepareMessage msg = new PrepareMessage(currentProposalNumber, currentProposalValue);
			broadcast(msg);			
		}
	}

	@Override
	public void postStep() {}
	
	@Override
	public String toString() {
		String s = "Node(" + this.ID + ") [";
		Iterator<Edge> edgeIter = this.outgoingConnections.iterator();
		while(edgeIter.hasNext()){
			Edge e = edgeIter.next();
			Node n = e.endNode;
			s+=n.ID+" ";
		}
		return s + "]";
	}

	@Override
	public void draw(Graphics g, PositionTransformation pt, boolean highlight) {
		String text = String.valueOf(this.ID);
		if (distinguished) {
			text += " (" + proposalsAccepted + ")";
		}
		super.drawNodeAsSquareWithText(g, pt, highlight, text, 25, Color.WHITE);
	}
	
	@Override
	public void checkRequirements() throws WrongConfigurationException {}

}
