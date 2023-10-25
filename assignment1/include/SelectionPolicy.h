#pragma once

class Graph;

class SelectionPolicy {
    public:
        virtual int select (const Graph& graph, int partyID) const = 0;
        virtual SelectionPolicy* clone() const = 0;
        virtual ~SelectionPolicy() = default;
 };

class MandatesSelectionPolicy: public SelectionPolicy{
    public:
        int select (const Graph& graph, int partyID) const override;
        SelectionPolicy* clone() const override;
 };

class EdgeWeightSelectionPolicy: public SelectionPolicy{
    public:
        int select (const Graph& graph, int partyID) const override;
        SelectionPolicy* clone() const override;
 };