package id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.port.in;

import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.application.dto.VariabelPokokDTO;
import id.ac.ui.cs.advprog.mysawitbe.modules.pembayaran.domain.VariableKey;

import java.util.List;

/**
 * Inbound port: read-only operations for wage variables.
 */
public interface VariabelPokokQueryUseCase {

    /** Returns all three wage variables with their current rates. */
    List<VariabelPokokDTO> getAllVariabelPokok();

    /** Returns the wage variable for the given key. */
    VariabelPokokDTO getVariabelPokok(VariableKey key);
}
